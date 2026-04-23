package org.ticketing.common.util;

import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.ticketing.common.exception.UnAuthorizedException;
import org.ticketing.config.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityUtil {
    public static Optional<UserDetailsImpl> getCurrentUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof UserDetailsImpl)
                .map(UserDetailsImpl.class::cast);
    }

    public static Optional<UUID> getCurrentUserId() {
        return getCurrentUser().map(UserDetailsImpl::getUuid);
    }

    public static UUID getCurrentUserIdOrThrow() {
        return getCurrentUserId().orElseThrow(UnAuthorizedException::new);
    }

    public static Optional<String> getCurrentUsername() {
        return getCurrentUser().map(UserDetailsImpl::getUsername);
    }
}
