package com.keystone.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketEventLogger {

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        log.info("WebSocket session connected");
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        log.info("WebSocket session disconnected");
    }
}
