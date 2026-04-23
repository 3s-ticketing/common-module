package org.iimsa.common.messaging.scheduler;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iimsa.common.domain.QInbox;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class InboxCleanupScheduler {
    private final JPAQueryFactory queryFactory;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시에 정리 작업 진행
    public void cleanupOldMessage() {
        // 7일 이전 목록 삭제
        long count = queryFactory
                .delete(QInbox.inbox)
                .where(QInbox.inbox.processedAt.before(LocalDateTime.now().minusWeeks(1L)))
                .execute();

        log.info("7일 경과 Inbox 내역 삭제: {}건 삭제됨", count);
    }
}
