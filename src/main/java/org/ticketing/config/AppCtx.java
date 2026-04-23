package org.ticketing.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.ticketing.common.exception.GlobalExceptionAdvice;
import org.ticketing.common.filter.MdcLoggingFilter;
import org.ticketing.common.response.CommonResponseAdvice;
import org.ticketing.config.event.EventConfig;
import org.ticketing.config.feign.FeignConfig;
import org.ticketing.config.json.JsonConfig;
import org.ticketing.config.kafka.KafkaConfig;
import org.ticketing.config.persistence.JPAConfig;
import org.ticketing.config.security.CustomAccessDeniedHandler;
import org.ticketing.config.security.CustomAuthenticationEntryPoint;
import org.ticketing.config.security.LoginFilter;
import org.ticketing.config.security.SecurityConfig;
import org.ticketing.config.web.PaginationConfig;
import org.ticketing.config.web.WebConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;

// 스프링 부트의 자동 설정 매커니즘에 참여하여 라이브러리 로드 시 자동 실행됨
@AutoConfiguration
@Import({
        FeignConfig.class,
        EventConfig.class,
        JPAConfig.class,
        JsonConfig.class,
        KafkaConfig.class,
        PaginationConfig.class,
        WebConfig.class
})
public class AppCtx {

    @Bean
    public LoginFilter loginFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return new LoginFilter(resolver);
    }

    @Bean
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new CustomAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public CustomAccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return new CustomAccessDeniedHandler(objectMapper);
    }

    @Bean
    public SecurityConfig securityConfig(LoginFilter loginFilter,
                                         CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                                         CustomAccessDeniedHandler accessDeniedHandler) {
        return new SecurityConfig(loginFilter, customAuthenticationEntryPoint, accessDeniedHandler);
    }

    // 전역 에러 출력 처리, GlobalExceptionAdvice로 등록된 빈이 없을때 기본 설정으로 등록됨
    @Bean
    public GlobalExceptionAdvice globalExceptionAdvice() {
        return new GlobalExceptionAdvice();
    }

    @Bean
    public CommonResponseAdvice commonResponseAdvice() {
        return new CommonResponseAdvice();
    }

    // MDC 기반의 로깅 추적을 위한 필터를 스프링 컨테이너에 등록함
    @Bean
    public FilterRegistrationBean<MdcLoggingFilter> mdcLoggingFilter() {
        FilterRegistrationBean<MdcLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new MdcLoggingFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE); // 가장 먼저 적용되도록 우선순위를 가장 높에 지정(가장 작은 정수범위)
        return registrationBean;
    }
}
