package com.keystone.service;

import com.keystone.dto.NotificationPageResponse;
import com.keystone.dto.NotificationResponse;
import com.keystone.dto.UnreadCountResponse;
import com.keystone.entity.Notification;
import com.keystone.entity.Part;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.NotificationType;
import com.keystone.enums.RelatedEntityType;
import com.keystone.enums.Role;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.NotificationRepository;
import com.keystone.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final String ALICE_EMAIL = "alice@keystone.com";
    private static final String BOB_EMAIL = "bob@keystone.com";

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkOrderAccessGuard workOrderAccessGuard;

    @Mock
    private NotificationCreator notificationCreator;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User alice;
    private User bob;
    private User manager;
    private Notification aliceNotification;

    @BeforeEach
    void setUp() {
        alice = User.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Tech")
                .userEmail(ALICE_EMAIL)
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();
        bob = User.builder()
                .id(2L)
                .firstName("Bob")
                .lastName("Tech")
                .userEmail(BOB_EMAIL)
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();
        manager = User.builder()
                .id(3L)
                .firstName("Mia")
                .lastName("Manager")
                .userEmail("mia@keystone.com")
                .role(Role.MANAGER)
                .enabled(true)
                .build();
        aliceNotification = Notification.builder()
                .id(10L)
                .recipient(alice)
                .type(NotificationType.WORK_ORDER_ASSIGNED)
                .title("Work order assigned")
                .message("You have been assigned work order WO-000123.")
                .relatedEntityType(RelatedEntityType.WORK_ORDER)
                .relatedEntityId(123L)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createNotification_ShouldPersistForRecipient() {
        when(notificationCreator.persist(
                eq(1L),
                eq(NotificationType.GENERAL),
                eq("Hello"),
                eq("Welcome"),
                isNull(),
                isNull()
        )).thenReturn(aliceNotification);

        NotificationResponse response = notificationService.createNotification(
                1L, NotificationType.GENERAL, "Hello", "Welcome", null, null);

        assertEquals(10L, response.getId());
        assertEquals(NotificationType.WORK_ORDER_ASSIGNED, response.getType());
        verify(notificationCreator).persist(1L, NotificationType.GENERAL, "Hello", "Welcome", null, null);
    }

    @Test
    void getNotifications_ShouldReturnOnlyCurrentUserPage() {
        when(workOrderAccessGuard.requireCurrentUser(ALICE_EMAIL)).thenReturn(alice);
        when(notificationRepository.searchForRecipient(eq(1L), eq(false), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(aliceNotification)));
        when(notificationRepository.countByRecipientIdAndReadFalse(1L)).thenReturn(1L);

        NotificationPageResponse page = notificationService.getNotifications(0, 20, false, null, ALICE_EMAIL);

        assertEquals(1, page.getContent().size());
        assertEquals(10L, page.getContent().get(0).getId());
        assertEquals(1L, page.getUnreadCount());
        verify(notificationRepository).searchForRecipient(eq(1L), eq(false), isNull(), any(Pageable.class));
    }

    @Test
    void getUnreadCount_ShouldUseCountQuery() {
        when(workOrderAccessGuard.requireCurrentUser(ALICE_EMAIL)).thenReturn(alice);
        when(notificationRepository.countByRecipientIdAndReadFalse(1L)).thenReturn(5L);

        UnreadCountResponse response = notificationService.getUnreadCount(ALICE_EMAIL);

        assertEquals(5L, response.getUnreadCount());
        verify(notificationRepository).countByRecipientIdAndReadFalse(1L);
        verify(notificationRepository, never()).searchForRecipient(any(), any(), any(), any());
    }

    @Test
    void markAsRead_WhenOwnedUnread_ShouldSetReadAt() {
        when(workOrderAccessGuard.requireCurrentUser(ALICE_EMAIL)).thenReturn(alice);
        when(notificationRepository.findByIdAndRecipientId(10L, 1L)).thenReturn(Optional.of(aliceNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(10L, ALICE_EMAIL);

        assertTrue(response.isRead());
        assertNotNull(response.getReadAt());
        verify(notificationRepository).save(aliceNotification);
    }

    @Test
    void markAsRead_WhenAlreadyRead_ShouldBeIdempotent() {
        aliceNotification.setRead(true);
        aliceNotification.setReadAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        when(workOrderAccessGuard.requireCurrentUser(ALICE_EMAIL)).thenReturn(alice);
        when(notificationRepository.findByIdAndRecipientId(10L, 1L)).thenReturn(Optional.of(aliceNotification));

        NotificationResponse response = notificationService.markAsRead(10L, ALICE_EMAIL);

        assertTrue(response.isRead());
        assertEquals(LocalDateTime.of(2026, 10, 1, 9, 0), response.getReadAt());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_WhenOwnedByAnotherUser_ShouldReturnNotFound() {
        when(workOrderAccessGuard.requireCurrentUser(ALICE_EMAIL)).thenReturn(alice);
        when(notificationRepository.findByIdAndRecipientId(20L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(20L, ALICE_EMAIL));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_WhenMissing_ShouldReturnNotFound() {
        when(workOrderAccessGuard.requireCurrentUser(ALICE_EMAIL)).thenReturn(alice);
        when(notificationRepository.findByIdAndRecipientId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(99L, ALICE_EMAIL));
    }

    @Test
    void markAllAsRead_ShouldBulkUpdateCurrentUserOnly() {
        when(workOrderAccessGuard.requireCurrentUser(ALICE_EMAIL)).thenReturn(alice);

        notificationService.markAllAsRead(ALICE_EMAIL);

        verify(notificationRepository).markAllReadForRecipient(eq(1L), any(LocalDateTime.class));
        verify(notificationRepository, never()).markAllReadForRecipient(eq(2L), any());
    }

    @Test
    void notifyWorkOrderAssigned_ShouldCreateAssignmentNotification() {
        WorkOrder workOrder = WorkOrder.builder()
                .id(123L)
                .workOrderNumber("WO-000123")
                .assignedTechnician(alice)
                .status(WorkOrderStatus.ASSIGNED)
                .build();

        notificationService.notifyWorkOrderAssigned(alice, workOrder);

        verify(notificationCreator).persist(
                1L,
                NotificationType.WORK_ORDER_ASSIGNED,
                "Work order assigned",
                "You have been assigned work order WO-000123.",
                RelatedEntityType.WORK_ORDER,
                123L
        );
    }

    @Test
    void notifyWorkOrderStatusChanged_WhenInProgress_ShouldNotifyAssignedTechnicianOnly() {
        WorkOrder workOrder = WorkOrder.builder()
                .id(123L)
                .workOrderNumber("WO-000123")
                .assignedTechnician(alice)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();

        notificationService.notifyWorkOrderStatusChanged(workOrder, WorkOrderStatus.IN_PROGRESS);

        verify(notificationCreator).persist(
                eq(1L),
                eq(NotificationType.WORK_ORDER_STATUS_CHANGED),
                eq("Work order status changed"),
                eq("Work order WO-000123 is now IN PROGRESS."),
                eq(RelatedEntityType.WORK_ORDER),
                eq(123L)
        );
        verify(userRepository, never()).findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(any(), anyBoolean());
    }

    @Test
    void notifyWorkOrderStatusChanged_WhenCompleted_ShouldNotifyTechnicianAndOperations() {
        WorkOrder workOrder = WorkOrder.builder()
                .id(123L)
                .workOrderNumber("WO-000123")
                .assignedTechnician(alice)
                .status(WorkOrderStatus.COMPLETED)
                .build();
        when(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.ADMIN, true)).thenReturn(List.of());
        when(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.MANAGER, true)).thenReturn(List.of(manager));

        notificationService.notifyWorkOrderStatusChanged(workOrder, WorkOrderStatus.COMPLETED);

        verify(notificationCreator).persist(
                eq(1L),
                eq(NotificationType.WORK_ORDER_COMPLETED),
                anyString(),
                anyString(),
                eq(RelatedEntityType.WORK_ORDER),
                eq(123L)
        );
        verify(notificationCreator).persist(
                eq(3L),
                eq(NotificationType.WORK_ORDER_COMPLETED),
                anyString(),
                anyString(),
                eq(RelatedEntityType.WORK_ORDER),
                eq(123L)
        );
    }

    @Test
    void notifyLowStockIfNeeded_WhenAtReorderLevel_ShouldNotifyOperationsOnce() {
        Part part = Part.builder()
                .id(44L)
                .partNumber("FLT-100")
                .quantityInStock(2)
                .reorderLevel(2)
                .build();
        when(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.ADMIN, true)).thenReturn(List.of());
        when(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.MANAGER, true)).thenReturn(List.of(manager));
        when(notificationRepository.existsByRecipientIdAndTypeAndRelatedEntityTypeAndRelatedEntityIdAndReadFalse(
                3L, NotificationType.PART_LOW_STOCK, RelatedEntityType.PART, 44L
        )).thenReturn(false);

        notificationService.notifyLowStockIfNeeded(part);

        verify(notificationCreator).persist(
                eq(3L),
                eq(NotificationType.PART_LOW_STOCK),
                eq("Part low stock"),
                contains("FLT-100"),
                eq(RelatedEntityType.PART),
                eq(44L)
        );
    }

    @Test
    void notifyLowStockIfNeeded_WhenUnreadAlreadyExists_ShouldNotDuplicate() {
        Part part = Part.builder()
                .id(44L)
                .partNumber("FLT-100")
                .quantityInStock(1)
                .reorderLevel(2)
                .build();
        when(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.ADMIN, true)).thenReturn(List.of());
        when(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.MANAGER, true)).thenReturn(List.of(manager));
        when(notificationRepository.existsByRecipientIdAndTypeAndRelatedEntityTypeAndRelatedEntityIdAndReadFalse(
                3L, NotificationType.PART_LOW_STOCK, RelatedEntityType.PART, 44L
        )).thenReturn(true);

        notificationService.notifyLowStockIfNeeded(part);

        verify(notificationCreator, never()).persist(any(), any(), any(), any(), any(), any());
    }

    @Test
    void notifyLowStockIfNeeded_WhenAboveReorder_ShouldSkip() {
        Part part = Part.builder()
                .id(44L)
                .partNumber("FLT-100")
                .quantityInStock(8)
                .reorderLevel(2)
                .build();

        notificationService.notifyLowStockIfNeeded(part);

        verifyNoInteractions(notificationCreator);
    }

    @Test
    void notifySlaIfNeeded_WhenAlreadyNotified_ShouldNotDuplicate() {
        WorkOrder workOrder = WorkOrder.builder()
                .id(123L)
                .workOrderNumber("WO-000123")
                .assignedTechnician(alice)
                .slaResolutionDueAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
        when(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.ADMIN, true)).thenReturn(List.of());
        when(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.MANAGER, true)).thenReturn(List.of());
        when(notificationRepository.existsByRecipientIdAndTypeAndRelatedEntityTypeAndRelatedEntityId(
                1L, NotificationType.SLA_BREACHED, RelatedEntityType.WORK_ORDER, 123L
        )).thenReturn(true);

        notificationService.notifySlaIfNeeded(workOrder);

        verify(notificationCreator, never()).persist(any(), any(), any(), any(), any(), any());
    }

    @Test
    void notifySlaIfNeeded_WhenBreached_ShouldNotifyOncePerRecipient() {
        WorkOrder workOrder = WorkOrder.builder()
                .id(123L)
                .workOrderNumber("WO-000123")
                .assignedTechnician(alice)
                .slaResolutionDueAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
        when(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.ADMIN, true)).thenReturn(List.of());
        when(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.MANAGER, true)).thenReturn(List.of(manager));
        when(notificationRepository.existsByRecipientIdAndTypeAndRelatedEntityTypeAndRelatedEntityId(
                anyLong(), eq(NotificationType.SLA_BREACHED), eq(RelatedEntityType.WORK_ORDER), eq(123L)
        )).thenReturn(false);

        notificationService.notifySlaIfNeeded(workOrder);

        ArgumentCaptor<Long> recipientCaptor = ArgumentCaptor.forClass(Long.class);
        verify(notificationCreator, times(2)).persist(
                recipientCaptor.capture(),
                eq(NotificationType.SLA_BREACHED),
                eq("SLA breached"),
                anyString(),
                eq(RelatedEntityType.WORK_ORDER),
                eq(123L)
        );
        assertEquals(List.of(1L, 3L), recipientCaptor.getAllValues());
    }

    @Test
    void getNotifications_WhenAnotherUser_DoesNotQueryTheirInbox() {
        when(workOrderAccessGuard.requireCurrentUser(BOB_EMAIL)).thenReturn(bob);
        when(notificationRepository.searchForRecipient(eq(2L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(notificationRepository.countByRecipientIdAndReadFalse(2L)).thenReturn(0L);

        notificationService.getNotifications(0, 20, null, null, BOB_EMAIL);

        verify(notificationRepository).searchForRecipient(eq(2L), isNull(), isNull(), any(Pageable.class));
        verify(notificationRepository, never()).searchForRecipient(eq(1L), any(), any(), any());
    }
}
