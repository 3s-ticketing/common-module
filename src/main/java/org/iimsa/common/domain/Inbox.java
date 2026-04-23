package org.iimsa.common.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@Access(AccessType.FIELD)
@Table(name = "P_INBOX", indexes = {
        @Index(name = "idx_inbox_message_group", columnList = "messageGroup"),
        @Index(name = "idx_inbox_processed_at", columnList = "processedAt")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class Inbox {
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(length = 36, name = "message_id")
    protected UUID id; // Outbox에 등록된 메세지 ID와 동일하게 유지

    @Column(length = 50)
    protected String messageGroup;

    @CreatedDate
    @Column(updatable = false)
    protected LocalDateTime processedAt;
}
