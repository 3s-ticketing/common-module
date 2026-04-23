package org.iimsa.config.web;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 우리가 만든 리졸버를 스프링의 아규먼트 리졸버 리스트에 추가합니다.
        resolvers.add(new CustomPageableResolver());
    }
}
