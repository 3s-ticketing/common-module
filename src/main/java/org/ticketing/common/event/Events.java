package org.ticketing.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Outbox 이벤트 발행 진입점.
 *
 * <p>두 개의 오버로드를 제공한다:
 * <ul>
 *   <li>{@link #trigger(String, String, String, String, Object)} —
 *       Kafka 파티션 키로 {@code domainId} 를 사용 (기본).</li>
 *   <li>{@link #trigger(String, String, String, String, Object, String)} —
 *       파티션 키를 명시. {@code domainId} 와 다른 값으로 파티셔닝해야 할 때
 *       (예: reservation 이벤트를 match 단위로 순서 보장하고 싶을 때 matchId 지정).</li>
 * </ul>
 */
@Slf4j
public class Events {
    private static KafkaTemplate<String, Object> kafkaTemplate;
    private static ApplicationEventPublisher eventPublisher;

    @Autowired
    public void init(KafkaTemplate<String, Object> kafkaTemplate, ApplicationEventPublisher eventPublisher) {
        Events.kafkaTemplate = kafkaTemplate;
        Events.eventPublisher = eventPublisher;
    }

    /**
     * Outbox 이벤트 발행 — Kafka 파티션 키로 {@code domainId} 사용.
     */
    public static void trigger(String correlationId, String domainType, String domainId, String eventType,
                               Object payload) {
        trigger(correlationId, domainType, domainId, eventType, payload, null);
    }

    /**
     * Outbox 이벤트 발행 — 파티션 키 명시.
     *
     * @param partitionKey {@code null} 이면 {@code domainId} 가 사용된다.
     */
    public static void trigger(String correlationId, String domainType, String domainId, String eventType,
                               Object payload, String partitionKey) {
        if (kafkaTemplate == null || eventPublisher == null) {
            log.warn("[Outbox] Events 가 초기화되지 않아 이벤트가 무시됩니다. correlationId={}, eventType={}",
                    correlationId, eventType);
            return;
        }
        eventPublisher.publishEvent(
                new OutboxEvent(correlationId, domainType, domainId, eventType, payload, partitionKey));
    }
}
