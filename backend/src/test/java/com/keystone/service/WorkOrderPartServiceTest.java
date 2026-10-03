package com.keystone.service;

import com.keystone.dto.CreateWorkOrderPartRequest;
import com.keystone.dto.UpdateWorkOrderPartRequest;
import com.keystone.dto.WorkOrderPartResponse;
import com.keystone.entity.Part;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.entity.WorkOrderPart;
import com.keystone.enums.PartStatus;
import com.keystone.enums.Role;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.PartRepository;
import com.keystone.repository.WorkOrderPartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderPartServiceTest {

    private static final String TECH_A_EMAIL = "tech.a@keystone.com";
    private static final String TECH_B_EMAIL = "tech.b@keystone.com";

    @Mock
    private WorkOrderPartRepository workOrderPartRepository;

    @Mock
    private PartRepository partRepository;

    @Mock
    private WorkOrderAccessGuard workOrderAccessGuard;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WorkOrderPartServiceImpl workOrderPartService;

    private User technicianA;
    private User technicianB;
    private WorkOrder assignedToA;
    private Part filter;

    @BeforeEach
    void setUp() {
        technicianA = User.builder()
                .id(25L)
                .firstName("Terry")
                .lastName("Alpha")
                .userEmail(TECH_A_EMAIL)
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();
        technicianB = User.builder()
                .id(26L)
                .firstName("Pat")
                .lastName("Bravo")
                .userEmail(TECH_B_EMAIL)
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();

        assignedToA = WorkOrder.builder()
                .id(7L)
                .workOrderNumber("WO-000007")
                .title("Replace HVAC filter")
                .assignedTechnician(technicianA)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();

        filter = Part.builder()
                .id(10L)
                .partNumber("FLT-100")
                .name("HVAC Filter")
                .unitCost(new BigDecimal("12.50"))
                .quantityInStock(8)
                .reorderLevel(2)
                .status(PartStatus.ACTIVE)
                .build();
    }

    @Test
    void addWorkOrderPart_WhenTechnicianOwnsWorkOrder_ShouldConsumeStockAndCaptureCost() {
        CreateWorkOrderPartRequest request = CreateWorkOrderPartRequest.builder()
                .partId(10L)
                .quantity(2)
                .build();
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        when(partRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(filter));
        when(workOrderPartRepository.save(any(WorkOrderPart.class))).thenAnswer(invocation -> {
            WorkOrderPart saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });
        when(workOrderPartRepository.findByIdAndWorkOrderIdWithRelations(50L, 7L))
                .thenAnswer(invocation -> Optional.of(usage(50L, 2, filter.getQuantityInStock())));

        WorkOrderPartResponse response = workOrderPartService.addWorkOrderPart(7L, request, TECH_A_EMAIL);

        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(partRepository).save(partCaptor.capture());
        assertEquals(6, partCaptor.getValue().getQuantityInStock());

        ArgumentCaptor<WorkOrderPart> usageCaptor = ArgumentCaptor.forClass(WorkOrderPart.class);
        verify(workOrderPartRepository).save(usageCaptor.capture());
        assertEquals(new BigDecimal("12.50"), usageCaptor.getValue().getUnitCostAtUsage());
        assertEquals(new BigDecimal("25.00"), usageCaptor.getValue().getTotalCost());
        assertEquals(technicianA, usageCaptor.getValue().getUsedBy());
        assertEquals(50L, response.getId());
        verify(notificationService).notifyLowStockIfNeeded(partCaptor.getValue());
    }

    @Test
    void addWorkOrderPart_WhenStockReachesReorderLevel_ShouldRequestLowStockNotification() {
        filter.setQuantityInStock(3);
        filter.setReorderLevel(2);
        CreateWorkOrderPartRequest request = CreateWorkOrderPartRequest.builder()
                .partId(10L)
                .quantity(1)
                .build();
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        when(partRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(filter));
        when(workOrderPartRepository.save(any(WorkOrderPart.class))).thenAnswer(invocation -> {
            WorkOrderPart saved = invocation.getArgument(0);
            saved.setId(51L);
            return saved;
        });
        when(workOrderPartRepository.findByIdAndWorkOrderIdWithRelations(51L, 7L))
                .thenAnswer(invocation -> Optional.of(usage(51L, 1, filter.getQuantityInStock())));

        workOrderPartService.addWorkOrderPart(7L, request, TECH_A_EMAIL);

        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(notificationService).notifyLowStockIfNeeded(partCaptor.capture());
        assertEquals(2, partCaptor.getValue().getQuantityInStock());
    }

    @Test
    void addWorkOrderPart_WhenInsufficientStock_ShouldConflict() {
        CreateWorkOrderPartRequest request = CreateWorkOrderPartRequest.builder()
                .partId(10L)
                .quantity(20)
                .build();
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        when(partRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(filter));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> workOrderPartService.addWorkOrderPart(7L, request, TECH_A_EMAIL)
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(workOrderPartRepository, never()).save(any());
        verify(notificationService, never()).notifyLowStockIfNeeded(any());
    }

    @Test
    void addWorkOrderPart_WhenQuantityInvalid_ShouldReject() {
        CreateWorkOrderPartRequest request = CreateWorkOrderPartRequest.builder()
                .partId(10L)
                .quantity(0)
                .build();
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> workOrderPartService.addWorkOrderPart(7L, request, TECH_A_EMAIL)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void addWorkOrderPart_WhenInactivePart_ShouldConflict() {
        filter.setStatus(PartStatus.INACTIVE);
        CreateWorkOrderPartRequest request = CreateWorkOrderPartRequest.builder()
                .partId(10L)
                .quantity(1)
                .build();
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        when(partRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(filter));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> workOrderPartService.addWorkOrderPart(7L, request, TECH_A_EMAIL)
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void addWorkOrderPart_WhenOtherTechnicianWorkOrder_ShouldNotLeak() {
        when(workOrderAccessGuard.requireCurrentUser(TECH_B_EMAIL)).thenReturn(technicianB);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_B_EMAIL))
                .thenThrow(new ResourceNotFoundException("Work order not found with id: 7"));

        assertThrows(
                ResourceNotFoundException.class,
                () -> workOrderPartService.addWorkOrderPart(
                        7L,
                        CreateWorkOrderPartRequest.builder().partId(10L).quantity(1).build(),
                        TECH_B_EMAIL
                )
        );
        verify(partRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void addWorkOrderPart_WhenClosed_ShouldConflict() {
        assignedToA.setStatus(WorkOrderStatus.CLOSED);
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        doThrow(new ApiException("Parts cannot be consumed on a closed or cancelled work order.", HttpStatus.CONFLICT))
                .when(workOrderAccessGuard).assertPartsMutable(assignedToA);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> workOrderPartService.addWorkOrderPart(
                        7L,
                        CreateWorkOrderPartRequest.builder().partId(10L).quantity(1).build(),
                        TECH_A_EMAIL
                )
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void addWorkOrderPart_WhenCancelled_ShouldConflict() {
        assignedToA.setStatus(WorkOrderStatus.CANCELLED);
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        doThrow(new ApiException("Parts cannot be consumed on a closed or cancelled work order.", HttpStatus.CONFLICT))
                .when(workOrderAccessGuard).assertPartsMutable(assignedToA);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> workOrderPartService.addWorkOrderPart(
                        7L,
                        CreateWorkOrderPartRequest.builder().partId(10L).quantity(1).build(),
                        TECH_A_EMAIL
                )
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void updateWorkOrderPart_WhenQuantityIncreases_ShouldConsumeDifference() {
        WorkOrderPart usage = usage(50L, 2, 8);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        when(workOrderPartRepository.findByIdAndWorkOrderIdWithRelations(50L, 7L)).thenReturn(Optional.of(usage));
        when(partRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(filter));
        when(workOrderPartRepository.save(any(WorkOrderPart.class))).thenReturn(usage);

        workOrderPartService.updateWorkOrderPart(
                7L,
                50L,
                UpdateWorkOrderPartRequest.builder().quantity(5).build(),
                TECH_A_EMAIL
        );

        assertEquals(5, filter.getQuantityInStock());
        assertEquals(5, usage.getQuantityUsed());
        assertEquals(new BigDecimal("62.50"), usage.getTotalCost());
    }

    @Test
    void updateWorkOrderPart_WhenQuantityDecreases_ShouldRestoreDifference() {
        WorkOrderPart usage = usage(50L, 5, 8);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        when(workOrderPartRepository.findByIdAndWorkOrderIdWithRelations(50L, 7L)).thenReturn(Optional.of(usage));
        when(partRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(filter));
        when(workOrderPartRepository.save(any(WorkOrderPart.class))).thenReturn(usage);

        workOrderPartService.updateWorkOrderPart(
                7L,
                50L,
                UpdateWorkOrderPartRequest.builder().quantity(2).build(),
                TECH_A_EMAIL
        );

        assertEquals(11, filter.getQuantityInStock());
        assertEquals(2, usage.getQuantityUsed());
        assertEquals(new BigDecimal("25.00"), usage.getTotalCost());
    }

    @Test
    void deleteWorkOrderPart_ShouldRestoreStock() {
        WorkOrderPart usage = usage(50L, 3, 8);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        when(workOrderPartRepository.findByIdAndWorkOrderIdWithRelations(50L, 7L)).thenReturn(Optional.of(usage));
        when(partRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(filter));

        workOrderPartService.deleteWorkOrderPart(7L, 50L, TECH_A_EMAIL);

        assertEquals(11, filter.getQuantityInStock());
        verify(workOrderPartRepository).delete(usage);
    }

    private WorkOrderPart usage(Long id, int quantity, int ignoredStock) {
        return WorkOrderPart.builder()
                .id(id)
                .workOrder(assignedToA)
                .part(filter)
                .quantityUsed(quantity)
                .unitCostAtUsage(new BigDecimal("12.50"))
                .totalCost(new BigDecimal("12.50").multiply(BigDecimal.valueOf(quantity)))
                .usedBy(technicianA)
                .usedAt(LocalDateTime.now())
                .build();
    }
}
