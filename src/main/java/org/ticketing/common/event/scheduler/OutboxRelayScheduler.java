package org.ticketing.common.event.scheduler;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ticketing.common.domain.Outbox;
import org.ticketing.common.domain.OutboxRepository;
import org.ticketing.common.domain.OutboxStatus;
import org.ticketing.common.domain.QOutbox;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class OutboxRelayScheduler {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final int MAX_RETRY_COUNT = 3;

    @Transactional
    @Scheduled(fixedDelay = 10000) // 10초에 한번씩 전송 미전송 또는 실패 메세지 재전송
    public void resendFailedMessages() {
        // PENDING, FAILED 상태이면서 retryCount가 3 미만 목록 조회
        List<Outbox> items = (List<Outbox>) outboxRepository.findAll(
                QOutbox.outbox.status.in(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED))
                        .and(QOutbox.outbox.retryCount.lt(MAX_RETRY_COUNT)));

        if (items.isEmpty()) {
            return;
        }

        log.info("재전송 대상 {}건 발견. 처리를 시작합니다.", items.size());

        for (Outbox outbox : items) {
            UUID targetId = outbox.getId();
            try {
                // 파티션 키: partitionKey 가 명시돼 있으면 그 값, 아니면 domainId
                kafkaTemplate.send(outbox.getEventType(), outbox.resolveKafkaKey(), outbox.getPayload())
                        .whenComplete((result, e) -> updateStatus(targetId, e == null));
            } catch (Exception e) {
                updateStatus(targetId, false);
                log.error("재전송 중 예외 발생: {}", outbox.getCorrelationId(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(UUID id, boolean isSuccess) {
        outboxRepository.findById(id).ifPresent(outbox -> {
            if (isSuccess) {
                outbox.complete();
                log.info("재전송 성공: {}", outbox.getCorrelationId());
            } else {
                outbox.fail();
                log.warn("재전송 실패 (현재 횟수: {}): {}", outbox.getRetryCount(), outbox.getCorrelationId());
            }
            outboxRepository.saveAndFlush(outbox);
        });
    }
}
