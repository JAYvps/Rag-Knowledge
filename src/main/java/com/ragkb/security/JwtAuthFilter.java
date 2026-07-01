package com.ragkb.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter
 *
 * Runs once per request, extracts JWT from Authorization header,
 * validates it, and sets the SecurityContext if valid.
 *
 * For public endpoints (login, register), this filter is skipped entirely
 * via the shouldNotFilter() method.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // These paths will skip this filter entirely
    private static final List<String> SKIP_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    /**
     * Tell Spring to skip this filter for certain paths.
     * This is the key fix: the filter won't even run for login/register,
     * so the request body is guaranteed to be untouched.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                if (jwtUtils.validateToken(token)) {
                    Long userId = jwtUtils.getUserId(token);
                    String username = jwtUtils.getUsername(token);
                    String role = jwtUtils.getRole(token);

                    UserDetailsImpl userDetails = new UserDetailsImpl(
                            userId, username, "", role
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT from "Authorization: Bearer xxx" header
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
