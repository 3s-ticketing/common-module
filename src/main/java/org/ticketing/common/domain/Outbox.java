package org.ticketing.common.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Builder
@Access(AccessType.FIELD)
@Table(name = "P_OUTBOX", indexes = {@Index(name = "idx_outbox_status", columnList = "status")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Outbox extends BaseEntity {
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(length = 36, name = "message_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;

    @Column(length = 64, nullable = false, unique = true)
    protected String correlationId; // SAGA 상관 ID - 같은 아이디를 가진 메세지는 동일한 처리 흐름에 묶여 있음

    @Column(length = 50, nullable = false)
    protected String domainType; // 도메인 종류

    @Column(length = 50, nullable = false)
    protected String domainId; // 도메인 식별자

    @Column(length = 100, nullable = false)
    protected String eventType; // 이벤트 타입, 카프카를 쓰게되면 Topic이 될 것

    @JdbcTypeCode(SqlTypes.JSON)
    protected String payload; // 전송한 메세지(JSON 형식)

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    protected OutboxStatus status = OutboxStatus.PENDING;

    @Builder.Default
    protected int retryCount = 0; // 재시도 카운트

    /**
     * Kafka 파티션 키. nullable.
     *
     * <p>null 이면 {@link #resolveKafkaKey()} 가 {@link #domainId} 로 fallback 한다.
     * 다른 도메인 단위로 파티션 순서 보장이 필요한 경우에만 명시 (예: reservation 이벤트를
     * matchId 로 파티셔닝하여 같은 경기 이벤트의 순서 보장).
     */
    @Column(length = 100)
    protected String partitionKey;

    public void complete() {
        this.status = OutboxStatus.PROCESSED;
    }

    public void fail() {
        this.status = OutboxStatus.FAILED;
        this.retryCount++;
    }

    /**
     * Kafka 발행 시 사용할 파티션 키.
     * {@link #partitionKey} 가 명시돼 있으면 그 값을, 아니면 {@link #domainId} 를 반환한다.
     */
    public String resolveKafkaKey() {
        return partitionKey != null ? partitionKey : domainId;
    }
}
