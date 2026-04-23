package org.ticketing.config.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

@Getter
@ToString
public class UserDetailsImpl implements UserDetails {

    private final UUID uuid;
    private final String username;
    private final String name;
    private final String email;
    private final String slackId;
    private final String roles;
    private final boolean enabled;

    @Builder
    public UserDetailsImpl(UUID uuid, String username, String name, String email, String slackId, String roles,
                           boolean enabled) {
        this.uuid = uuid;
        this.username = username;
        this.name = name;
        this.email = email;
        this.slackId = slackId;
        this.roles = roles;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        /**
         * ROLE_MASTER가 ROLE_USER에서
         * - ROLE_HUB_MANAGER
         * - ROLE_HUB_DRIVER
         * - ROLE_STORE_DRIVER
         * - ROLE_STORE_MANAGER
         * 로 역할 승인을 합니다.
         */
        if (!StringUtils.hasText(roles)) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
