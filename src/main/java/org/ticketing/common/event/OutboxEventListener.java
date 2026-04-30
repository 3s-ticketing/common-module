package org.ticketing.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ticketing.common.domain.Outbox;
import org.ticketing.common.domain.OutboxRepository;
import org.ticketing.common.domain.OutboxStatus;
import org.ticketing.common.domain.QOutbox;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
public class OutboxEventListener {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY_COUNT = 3; // 재시도 최대 횟수

    // Outbox 기록
    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordOutbox(OutboxEvent event) {

        if (outboxRepository.exists(QOutbox.outbox.correlationId.eq(event.correlationId()))) {
            log.warn("이미 존재하는 correlationId 입니다.: {}", event.correlationId());
            return;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(event.payload());

            Outbox outbox = Outbox.builder()
                    .correlationId(event.correlationId())
                    .domainType(event.domainType())
                    .domainId(event.domainId())
                    .eventType(event.eventType())
                    .payload(jsonPayload)
                    .partitionKey(event.partitionKey()) // null 이면 publish 시 domainId 로 fallback
                    .status(OutboxStatus.PENDING) // 메세지 전송전 단계는 PENDING, 전송 완료 후는 PROCESSED호 변경됨
                    .build();
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Output payload 직렬화 실패: {}", event.correlationId(), e);
        }
    }

    // 메세지 전송(Kafka)
    // fallbackExecution = true, @Transactional이 없는 환경(단순 메시지 전송)에서도 실행 보장
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(OutboxEvent event) {
        outboxRepository.findByCorrelationId(event.correlationId()).ifPresent(outbox -> {
            // Kafka 메시지에 ID 헤더 추가
            // 파티션 키: partitionKey 가 명시돼 있으면 그 값, 아니면 domainId
            ProducerRecord<String, Object> record = new ProducerRecord<>(
                    outbox.getEventType(),
                    outbox.resolveKafkaKey(),
                    outbox.getPayload()
            );
            record.headers().add("message_id", outbox.getId().toString().getBytes());

            kafkaTemplate.send(record)
                    .whenComplete((result, e) -> {
                        if (e == null) {
                            handleSuccess(event.correlationId());
                        } else {
                            handleFailure(event, e);
                        }
                    });
        });
    }

    // 성공 처리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSuccess(String correlationId) {
        outboxRepository.findByCorrelationId(correlationId).ifPresent(outbox -> {
            outbox.complete();
            outboxRepository.save(outbox);
            log.info("Outbox 메세지 전송 및 상태 완료 변경 성공: {}", correlationId);
        });

    }

    /**
     * 실패 처리 최대 재시도 횟수 초과 시 DLT(Dead Letter Topic)로 메시지 발행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailure(OutboxEvent event, Throwable e) {
        outboxRepository.findByCorrelationId(event.correlationId()).ifPresent(outbox -> {
            outbox.fail();
            outboxRepository.saveAndFlush(outbox); // 횟수와 FAILED 상태 즉시 반영

            if (outbox.getRetryCount() >= MAX_RETRY_COUNT) {
                log.error("최대 재시도 횟수 초과(Total: {}). DLT로 격리합니다: {}", outbox.getRetryCount(), event.correlationId());
                sendToDlt(outbox);
            } else {
                log.warn("메세지 전송 실패 (재시도 예정 {}/{}): {}", outbox.getRetryCount(), MAX_RETRY_COUNT,
                        event.correlationId());
            }
        });
    }

    // DLT 전송 — 파티션 키는 원 메시지와 동일하게 resolveKafkaKey() 사용
    private void sendToDlt(Outbox outbox) {
        String dltTopic = outbox.getEventType() + ".DLT";
        try {
            // DLT 전송은 재시도 없이 1회만 시도, 실패 시 에러 로그만 기록
            kafkaTemplate.send(dltTopic, outbox.resolveKafkaKey(), outbox.getPayload())
                    .whenComplete((res, e) -> {
                        if (e != null) {
                            log.error("DLT 전송 실패: {}", outbox.getCorrelationId(), e);
                        } else {
                            log.info("DLT 전송 성공: {}", outbox.getCorrelationId());
                        }
                    });
        } catch (Exception e) {
            log.error("DLT 전송 중 예외 발생: {}", outbox.getCorrelationId(), e);
        }
    }
}
