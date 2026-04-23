package org.ticketing.config.web;

import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

//chore: 페이징 객체의 유효성 검사 위한 CustomPageableResolver 작성
public class CustomPageableResolver extends PageableHandlerMethodArgumentResolver {

    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 30, 50);
    private static final int DEFAULT_SIZE = 10;

    @Override
    public Pageable resolveArgument(
            MethodParameter methodParameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        Pageable pageable = super.resolveArgument(
                methodParameter, mavContainer, webRequest, binderFactory);

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();

        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            size = DEFAULT_SIZE;
        }

        return PageRequest.of(page, size, pageable.getSort());
    }
}
