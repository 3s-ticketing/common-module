package org.ticketing.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Slf4j
@Component
public class LoginFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_ROLES = "X-User-Roles";
    private static final String HEADER_EMAIL = "X-User-Email";
    private static final String HEADER_SLACK_ID = "X-User-Slack-Id";
    private static final String HEADER_USER_NAME = "X-User-Name";
    private static final String HEADER_ENABLED = "X-User-Enabled";

    private final HandlerExceptionResolver resolver;

    public LoginFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            SecurityContextHolder.clearContext();

            doLogin(request);
        } catch (DisabledException e) {
            log.warn("[LoginFilter] Access Denied: {}", e.getMessage());
            resolver.resolveException(request, response, null, e);
            return;
        } catch (Exception e) {
            log.error("인증에 실패하였습니다: {}", e.getMessage(), e);
            resolver.resolveException(request, response, null, e);
            return;

        }

        filterChain.doFilter(request, response);
    }

    private void doLogin(HttpServletRequest request) {
        String userId = request.getHeader(HEADER_USER_ID);
        String username = request.getHeader(HEADER_EMAIL);

        // 직접 접근 시 헤더가 없으므로 여기서 즉시 return
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(username)) {
            return;
        }

        String name = request.getHeader(HEADER_USER_NAME);
        String email = request.getHeader(HEADER_EMAIL);
        String slackId = request.getHeader(HEADER_SLACK_ID);
        String roles = request.getHeader(HEADER_ROLES);
        String enabled = request.getHeader(HEADER_ENABLED);

        if (StringUtils.hasText(name)) {
            try {
                name = URLDecoder.decode(name, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("Failed to decode user name: " + name, e);
            }
        }

        try {
            // UUID 파싱 전 trim()으로 불필요한 공백 제거
            String cleanUserId = userId.trim();

            UserDetails userDetails = UserDetailsImpl.builder()
                    .uuid(UUID.fromString(cleanUserId))
                    .username(username)
                    .email(email)
                    .slackId(slackId)
                    .name(name)
                    .roles(roles)
                    .enabled("true".equalsIgnoreCase(enabled))
                    .build();

            if (!userDetails.isEnabled()) {
                String errorMsg = "승인 대기 중이거나 탈퇴한 사용자입니다.";
                log.warn("[LoginFilter] 접근이 제한된 사용자 입니다: {}, 사유: {}", cleanUserId, errorMsg);
                throw new DisabledException(errorMsg);
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (IllegalArgumentException e) {
            // 잘못된 UUID 형식이 들어와도 401을 던지지 않고 익명 상태로 진행시킵니다.
            log.warn("Invalid UUID format from Gateway header: " + userId, e);
        }
    }
}
