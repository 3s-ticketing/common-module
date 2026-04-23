package org.iimsa.common.event;

public record OutboxEvent(
        String correlationId,
        String domainType,
        String domainId,
        String eventType,
        Object payload
) {
}
