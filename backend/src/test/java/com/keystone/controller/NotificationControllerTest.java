package com.keystone.controller;

import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.NotificationPageResponse;
import com.keystone.dto.NotificationResponse;
import com.keystone.dto.UnreadCountResponse;
import com.keystone.enums.NotificationType;
import com.keystone.enums.RelatedEntityType;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.service.AuthorizationService;
import com.keystone.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private AuthorizationService authorizationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(username = "alice@keystone.com", roles = {"TECHNICIAN"})
    void getNotifications_ShouldReturnPageForAuthenticatedUser() throws Exception {
        when(notificationService.getNotifications(0, 20, null, null, "alice@keystone.com"))
                .thenReturn(NotificationPageResponse.builder()
                        .content(List.of(sampleNotification(10L, false)))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .unreadCount(1)
                        .build());

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].title").value("Work order assigned"))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        verify(notificationService).getNotifications(0, 20, null, null, "alice@keystone.com");
    }

    @Test
    @WithMockUser(username = "alice@keystone.com", roles = {"TECHNICIAN"})
    void getNotifications_ShouldHonorPaginationAndUnreadFilter() throws Exception {
        when(notificationService.getNotifications(1, 5, false, NotificationType.WORK_ORDER_ASSIGNED, "alice@keystone.com"))
                .thenReturn(NotificationPageResponse.builder()
                        .content(List.of())
                        .page(1)
                        .size(5)
                        .totalElements(0)
                        .totalPages(0)
                        .unreadCount(0)
                        .build());

        mockMvc.perform(get("/api/notifications")
                        .param("page", "1")
                        .param("size", "5")
                        .param("read", "false")
                        .param("type", "WORK_ORDER_ASSIGNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));

        verify(notificationService).getNotifications(
                1, 5, false, NotificationType.WORK_ORDER_ASSIGNED, "alice@keystone.com");
    }

    @Test
    @WithMockUser(username = "alice@keystone.com", roles = {"TECHNICIAN"})
    void getNotifications_WhenTypeInvalid_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/notifications").param("type", "NOT_A_TYPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice@keystone.com", roles = {"TECHNICIAN"})
    void getUnreadCount_ShouldReturnCount() throws Exception {
        when(notificationService.getUnreadCount("alice@keystone.com"))
                .thenReturn(UnreadCountResponse.builder().unreadCount(4).build());

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(4));
    }

    @Test
    @WithMockUser(username = "alice@keystone.com", roles = {"TECHNICIAN"})
    void markAsRead_ShouldUseAuthenticatedUsername() throws Exception {
        when(notificationService.markAsRead(10L, "alice@keystone.com"))
                .thenReturn(sampleNotification(10L, true));

        mockMvc.perform(patch("/api/notifications/10/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        verify(notificationService).markAsRead(10L, "alice@keystone.com");
    }

    @Test
    @WithMockUser(username = "alice@keystone.com", roles = {"TECHNICIAN"})
    void markAsRead_WhenNotOwned_ShouldReturn404() throws Exception {
        when(notificationService.markAsRead(20L, "alice@keystone.com"))
                .thenThrow(new ResourceNotFoundException("Notification not found with id: 20"));

        mockMvc.perform(patch("/api/notifications/20/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found with id: 20"));
    }

    @Test
    @WithMockUser(username = "alice@keystone.com", roles = {"TECHNICIAN"})
    void markAllAsRead_ShouldReturnZeroUnread() throws Exception {
        when(notificationService.getUnreadCount("alice@keystone.com"))
                .thenReturn(UnreadCountResponse.builder().unreadCount(0).build());

        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        verify(notificationService).markAllAsRead("alice@keystone.com");
    }

    @Test
    @WithMockUser(username = "alice@keystone.com", roles = {"TECHNICIAN"})
    void getNotifications_ShouldIgnoreClientRecipientId() throws Exception {
        when(notificationService.getNotifications(eq(0), eq(20), isNull(), isNull(), eq("alice@keystone.com")))
                .thenReturn(NotificationPageResponse.builder()
                        .content(List.of())
                        .page(0)
                        .size(20)
                        .totalElements(0)
                        .totalPages(0)
                        .unreadCount(0)
                        .build());

        mockMvc.perform(get("/api/notifications").param("recipientId", "99"))
                .andExpect(status().isOk());

        verify(notificationService).getNotifications(0, 20, null, null, "alice@keystone.com");
    }

    private NotificationResponse sampleNotification(Long id, boolean read) {
        return NotificationResponse.builder()
                .id(id)
                .type(NotificationType.WORK_ORDER_ASSIGNED)
                .title("Work order assigned")
                .message("You have been assigned work order WO-000123.")
                .relatedEntityType(RelatedEntityType.WORK_ORDER)
                .relatedEntityId(123L)
                .read(read)
                .createdAt(LocalDateTime.of(2026, 10, 3, 12, 0))
                .readAt(read ? LocalDateTime.of(2026, 10, 3, 12, 5) : null)
                .build();
    }
}
