package com.keystone.config;

public final class WebSocketDestinations {

    public static final String ENDPOINT = "/ws";
    public static final String APPLICATION_PREFIX = "/app";
    public static final String TOPIC_PREFIX = "/topic";
    public static final String QUEUE_PREFIX = "/queue";
    public static final String USER_PREFIX = "/user";
    public static final String USER_QUEUE_NOTIFICATIONS = "/queue/notifications";
    public static final String CLIENT_SUBSCRIBE_NOTIFICATIONS = "/user/queue/notifications";

    private WebSocketDestinations() {
    }
}
