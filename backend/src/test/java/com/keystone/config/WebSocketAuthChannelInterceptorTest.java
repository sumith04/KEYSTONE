package com.keystone.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthChannelInterceptor(
                jwtTokenProvider, customUserDetailsService, tokenBlacklistService);
    }

    @Test
    void connect_WhenMissingJwt_ShouldReject() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null, null);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, messageChannel));
        verifyNoInteractions(customUserDetailsService);
    }

    @Test
    void connect_WhenInvalidJwt_ShouldReject() {
        when(tokenBlacklistService.isRevoked("bad-token")).thenReturn(false);
        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Bearer bad-token", null);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, messageChannel));
        verifyNoInteractions(customUserDetailsService);
    }

    @Test
    void connect_WhenRevokedJwt_ShouldReject() {
        when(tokenBlacklistService.isRevoked("revoked-token")).thenReturn(true);
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Bearer revoked-token", null);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, messageChannel));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void connect_WhenValidJwt_ShouldSetAuthenticatedPrincipal() {
        UserDetails userDetails = User.builder()
                .username("alice@keystone.com")
                .password("n/a")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN")))
                .build();
        when(tokenBlacklistService.isRevoked("good-token")).thenReturn(false);
        when(jwtTokenProvider.validateToken("good-token")).thenReturn(true);
        when(jwtTokenProvider.getUserEmailFromToken("good-token")).thenReturn("alice@keystone.com");
        when(customUserDetailsService.loadUserByUsername("alice@keystone.com")).thenReturn(userDetails);

        Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Bearer good-token", null);
        Message<?> result = interceptor.preSend(message, messageChannel);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNotNull(accessor.getUser());
        assertEquals("alice@keystone.com", accessor.getUser().getName());
        assertTrue(accessor.getUser() instanceof Authentication);
    }

    @Test
    void subscribe_WhenOwnNotificationQueue_ShouldAllow() {
        Message<byte[]> message = stompMessage(
                StompCommand.SUBSCRIBE, null, WebSocketDestinations.CLIENT_SUBSCRIBE_NOTIFICATIONS);

        assertSame(message, interceptor.preSend(message, messageChannel));
    }

    @Test
    void subscribe_WhenSharedTopic_ShouldReject() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, null, "/topic/notifications");

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void subscribe_WhenAnotherUserQueue_ShouldReject() {
        Message<byte[]> message = stompMessage(
                StompCommand.SUBSCRIBE, null, "/user/bob@keystone.com/queue/notifications");

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void send_WhenClientPublishes_ShouldReject() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, null, "/app/notifications");

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, messageChannel));
    }

    private Message<byte[]> stompMessage(StompCommand command, String authorization, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        if (destination != null) {
            accessor.setDestination(destination);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
