package org.iimsa.config.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.concurrent.Executor;
import org.iimsa.common.domain.InboxRepository;
import org.iimsa.common.domain.OutboxRepository;
import org.iimsa.common.event.Events;
import org.iimsa.common.event.OutboxEventListener;
import org.iimsa.common.event.scheduler.OutboxRelayScheduler;
import org.iimsa.common.messaging.advice.InboxAdvice;
import org.iimsa.common.messaging.scheduler.InboxCleanupScheduler;
import org.iimsa.common.util.MdcTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@EnableAsync
@Configuration
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class EventConfig implements AsyncConfigurer {

    @Bean
    public Events events() {
        return new Events();
    }

    // 비동기 적용시 생성될 스레드 풀 설정
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);        // 기본 스레드 수
        executor.setMaxPoolSize(50);        // 최대 스레드 수
        executor.setQueueCapacity(100);     // 대기 큐 용량
        executor.setThreadNamePrefix("Async-"); // 스레드 이름 접두사

        // MdcTaskDecorator 등록
        executor.setTaskDecorator(new MdcTaskDecorator());

        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }

    @Bean
    public OutboxEventListener outboxEventListener(OutboxRepository outboxRepository,
                                                   KafkaTemplate<String, Object> kafkaTemplate,
                                                   ObjectMapper objectMapper) {
        return new OutboxEventListener(outboxRepository, kafkaTemplate, objectMapper);
    }

    @Bean
    public OutboxRelayScheduler OutboxRelayScheduler(OutboxRepository outboxRepository,
                                                     KafkaTemplate<String, Object> kafkaTemplate) {
        return new OutboxRelayScheduler(outboxRepository, kafkaTemplate);
    }

    @Bean
    public InboxAdvice inboxAdvice(InboxRepository inboxRepository) {
        return new InboxAdvice(inboxRepository);
    }

    @Bean
    public InboxCleanupScheduler inboxCleanupScheduler(JPAQueryFactory jpaQueryFactory) {
        return new InboxCleanupScheduler(jpaQueryFactory);
    }
}
