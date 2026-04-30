package org.ticketing.common.event;

/**
 * Outbox 발행 시 application event 로 전달되는 페이로드.
 *
 * <p>{@code partitionKey} 가 {@code null} 이면 {@code domainId} 가 Kafka 파티션 키로 사용된다.
 * 같은 경기 이벤트의 순서 보장처럼 별도 키가 필요한 경우에만 명시한다.
 */
public record OutboxEvent(
        String correlationId,
        String domainType,
        String domainId,
        String eventType,
        Object payload,
        String partitionKey
) {
}
