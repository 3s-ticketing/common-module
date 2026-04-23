package org.ticketing.common.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.ticketing.common.util.SecurityUtil;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.StringUtils;

@Getter
@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    @Column(updatable = false)
    protected LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    protected LocalDateTime modifiedAt;

    protected LocalDateTime deletedAt;

    @CreatedBy
    @Column(updatable = false)
    protected String createdBy;

    @LastModifiedBy
    @Column(insertable = false)
    protected String modifiedBy;

    @Column(insertable = false)
    protected String deletedBy;

    protected void delete(String deletedBy) {
        // 이미 삭제된 경우 중복 처리 방지
        if (this.deletedAt != null) {
            return;
        }

        this.deletedBy = StringUtils.hasText(deletedBy)
                ? deletedBy
                : SecurityUtil.getCurrentUsername().orElse("SYSTEM");

        this.deletedAt = LocalDateTime.now();
    }
}