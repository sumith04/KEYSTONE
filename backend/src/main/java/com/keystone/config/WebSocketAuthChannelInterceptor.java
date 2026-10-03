package com.keystone.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticateConnect(accessor);
            return message;
        }
        if (StompCommand.SEND.equals(command)) {
            rejectClientPublish();
        }
        if (StompCommand.SUBSCRIBE.equals(command)) {
            assertAllowedSubscription(accessor);
        }
        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String token = extractBearerToken(accessor);
        if (!StringUtils.hasText(token)
                || tokenBlacklistService.isRevoked(token)
                || !jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket CONNECT rejected: missing, revoked, or invalid JWT");
            throw new MessageDeliveryException("WebSocket authentication failed");
        }

        try {
            String userEmail = jwtTokenProvider.getUserEmailFromToken(token);
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            accessor.setUser(authentication);
            log.debug("WebSocket CONNECT authenticated");
        } catch (Exception ex) {
            log.warn("WebSocket CONNECT rejected: {}", ex.getMessage());
            throw new MessageDeliveryException("WebSocket authentication failed");
        }
    }

    private void rejectClientPublish() {
        log.warn("WebSocket SEND rejected: clients cannot publish notifications");
        throw new MessageDeliveryException("Clients cannot publish WebSocket messages");
    }

    private void assertAllowedSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (WebSocketDestinations.CLIENT_SUBSCRIBE_NOTIFICATIONS.equals(destination)) {
            return;
        }
        log.warn("WebSocket SUBSCRIBE rejected for destination {}", destination);
        throw new MessageDeliveryException("Subscription is not permitted");
    }

    private String extractBearerToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
