package org.iimsa.common.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface OutboxRepository extends JpaRepository<Outbox, UUID>, QuerydslPredicateExecutor<Outbox> {
    List<Outbox> findByStatus(OutboxStatus status);

    Optional<Outbox> findByCorrelationId(String correlationId);
}
