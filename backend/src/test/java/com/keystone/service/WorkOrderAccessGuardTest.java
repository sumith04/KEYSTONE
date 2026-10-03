package com.keystone.service;

import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.Role;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderAccessGuardTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkOrderAccessGuard workOrderAccessGuard;

    private User technicianA;
    private User technicianB;
    private WorkOrder assignedToA;

    @BeforeEach
    void setUp() {
        technicianA = User.builder().id(25L).userEmail("tech.a@keystone.com").role(Role.TECHNICIAN).enabled(true).build();
        technicianB = User.builder().id(26L).userEmail("tech.b@keystone.com").role(Role.TECHNICIAN).enabled(true).build();
        assignedToA = WorkOrder.builder()
                .id(7L)
                .assignedTechnician(technicianA)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();
    }

    @Test
    void requireAccessibleWorkOrder_WhenOtherTechnician_Should404() {
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(assignedToA));
        when(userRepository.findByUserEmail("tech.b@keystone.com")).thenReturn(Optional.of(technicianB));

        assertThrows(
                ResourceNotFoundException.class,
                () -> workOrderAccessGuard.requireAccessibleWorkOrder(7L, "tech.b@keystone.com")
        );
    }

    @Test
    void assertPartsMutable_WhenClosedOrCancelled_ShouldConflict() {
        assignedToA.setStatus(WorkOrderStatus.CLOSED);
        ApiException closed = assertThrows(ApiException.class, () -> workOrderAccessGuard.assertPartsMutable(assignedToA));
        assertEquals(HttpStatus.CONFLICT, closed.getStatus());

        assignedToA.setStatus(WorkOrderStatus.CANCELLED);
        ApiException cancelled = assertThrows(ApiException.class, () -> workOrderAccessGuard.assertPartsMutable(assignedToA));
        assertEquals(HttpStatus.CONFLICT, cancelled.getStatus());
    }

    @Test
    void assertTimeLogsMutable_WhenClosedOrCancelled_ShouldConflict() {
        assignedToA.setStatus(WorkOrderStatus.CLOSED);
        ApiException closed = assertThrows(ApiException.class, () -> workOrderAccessGuard.assertTimeLogsMutable(assignedToA));
        assertEquals(HttpStatus.CONFLICT, closed.getStatus());

        assignedToA.setStatus(WorkOrderStatus.CANCELLED);
        ApiException cancelled = assertThrows(ApiException.class, () -> workOrderAccessGuard.assertTimeLogsMutable(assignedToA));
        assertEquals(HttpStatus.CONFLICT, cancelled.getStatus());
    }
}
