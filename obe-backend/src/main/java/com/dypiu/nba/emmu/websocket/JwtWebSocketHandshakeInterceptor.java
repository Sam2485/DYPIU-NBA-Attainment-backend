package com.dypiu.nba.emmu.websocket;

import com.dypiu.nba.security.CustomUserDetailsService;
import com.dypiu.nba.security.JwtTokenProvider;
import com.dypiu.nba.security.TokenRevocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PRINCIPAL_ATTR = "WEBSOCKET_PRINCIPAL";

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenRevocationService tokenRevocationService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        try {
            String jwt = extractJwt(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt) && !tokenRevocationService.isRevoked(jwt)) {
                String username = tokenProvider.getUsernameFromJwt(jwt);
                Date issuedAt = tokenProvider.getIssuedAtFromJwt(jwt);

                if (tokenRevocationService.isUserTokenRevoked(username, issuedAt)) {
                    log.warn("[WebSocketHandshake] Token revoked for user: {}", username);
                    return true; // Allow anonymous fallback
                }

                String activeRole = tokenProvider.getActiveRoleFromJwt(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities;
                if (StringUtils.hasText(activeRole)) {
                    String cleanRole = activeRole.toUpperCase().startsWith("ROLE_")
                            ? activeRole.toUpperCase()
                            : "ROLE_" + activeRole.toUpperCase();
                    authorities = java.util.Collections.singletonList(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(cleanRole));
                } else {
                    authorities = userDetails.getAuthorities();
                }

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities);

                attributes.put(PRINCIPAL_ATTR, auth);
                log.info("[WebSocketHandshake] Authenticated WebSocket connection for user: {}, role: {}",
                        username, activeRole != null ? activeRole : userDetails.getAuthorities());
            }
        } catch (Exception e) {
            log.debug("[WebSocketHandshake] Non-fatal auth extraction issue: {}", e.getMessage());
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private String extractJwt(ServerHttpRequest request) {
        URI uri = request.getURI();
        String query = uri.getQuery();
        if (StringUtils.hasText(query)) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring(6).trim();
                }
            }
        }

        if (request instanceof ServletServerHttpRequest servletRequest) {
            String bearer = servletRequest.getServletRequest().getHeader("Authorization");
            if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                return bearer.substring(7).trim();
            }
        }

        return null;
    }
}
