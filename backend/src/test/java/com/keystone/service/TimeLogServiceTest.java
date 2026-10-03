package com.keystone.service;

import com.keystone.dto.CreateTimeLogRequest;
import com.keystone.dto.TimeLogResponse;
import com.keystone.dto.UpdateTimeLogRequest;
import com.keystone.entity.TimeLog;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.Role;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.TimeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeLogServiceTest {

    private static final String TECH_A_EMAIL = "tech.a@keystone.com";
    private static final String TECH_B_EMAIL = "tech.b@keystone.com";

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private WorkOrderAccessGuard workOrderAccessGuard;

    @InjectMocks
    private TimeLogServiceImpl timeLogService;

    private User technicianA;
    private User technicianB;
    private WorkOrder assignedToA;

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
    }

    @Test
    void addTimeLog_WhenOwnWorkOrder_ShouldUseAuthenticatedTechnicianAndCalculateDuration() {
        CreateTimeLogRequest request = CreateTimeLogRequest.builder()
                .startTime(LocalDateTime.of(2026, 10, 3, 9, 0))
                .endTime(LocalDateTime.of(2026, 10, 3, 11, 30))
                .notes("On site")
                .build();
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        when(timeLogRepository.existsOverlappingLog(eq(25L), any(), any(), isNull())).thenReturn(false);
        when(timeLogRepository.save(any(TimeLog.class))).thenAnswer(invocation -> {
            TimeLog saved = invocation.getArgument(0);
            saved.setId(80L);
            return saved;
        });
        when(timeLogRepository.findByIdAndWorkOrderIdWithRelations(80L, 7L))
                .thenAnswer(invocation -> Optional.of(savedLog(80L, technicianA, 150)));

        TimeLogResponse response = timeLogService.addTimeLog(7L, request, TECH_A_EMAIL);

        ArgumentCaptor<TimeLog> captor = ArgumentCaptor.forClass(TimeLog.class);
        verify(timeLogRepository).save(captor.capture());
        assertEquals(technicianA, captor.getValue().getTechnician());
        assertEquals(150, captor.getValue().getDurationMinutes());
        assertEquals(150, response.getDurationMinutes());
        assertEquals(25L, response.getTechnicianId());
    }

    @Test
    void addTimeLog_WhenOtherTechnicianWorkOrder_ShouldNotLeak() {
        when(workOrderAccessGuard.requireCurrentUser(TECH_B_EMAIL)).thenReturn(technicianB);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_B_EMAIL))
                .thenThrow(new ResourceNotFoundException("Work order not found with id: 7"));

        assertThrows(
                ResourceNotFoundException.class,
                () -> timeLogService.addTimeLog(7L, validRequest(), TECH_B_EMAIL)
        );
        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void addTimeLog_WhenInvalidRange_ShouldReject() {
        CreateTimeLogRequest request = CreateTimeLogRequest.builder()
                .startTime(LocalDateTime.of(2026, 10, 3, 11, 0))
                .endTime(LocalDateTime.of(2026, 10, 3, 9, 0))
                .build();
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);

        ApiException exception = assertThrows(ApiException.class, () -> timeLogService.addTimeLog(7L, request, TECH_A_EMAIL));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void addTimeLog_WhenClosed_ShouldConflict() {
        assignedToA.setStatus(WorkOrderStatus.CLOSED);
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        doThrow(new ApiException("Time logs cannot be created or changed on a closed or cancelled work order.", HttpStatus.CONFLICT))
                .when(workOrderAccessGuard).assertTimeLogsMutable(assignedToA);

        ApiException exception = assertThrows(ApiException.class, () -> timeLogService.addTimeLog(7L, validRequest(), TECH_A_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void addTimeLog_WhenCancelled_ShouldConflict() {
        assignedToA.setStatus(WorkOrderStatus.CANCELLED);
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        doThrow(new ApiException("Time logs cannot be created or changed on a closed or cancelled work order.", HttpStatus.CONFLICT))
                .when(workOrderAccessGuard).assertTimeLogsMutable(assignedToA);

        ApiException exception = assertThrows(ApiException.class, () -> timeLogService.addTimeLog(7L, validRequest(), TECH_A_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void addTimeLog_WhenOverlap_ShouldConflict() {
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        when(timeLogRepository.existsOverlappingLog(eq(25L), any(), any(), isNull())).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> timeLogService.addTimeLog(7L, validRequest(), TECH_A_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void calculateDurationMinutes_ShouldUseBackendMath() {
        int minutes = timeLogService.calculateDurationMinutes(
                LocalDateTime.of(2026, 10, 3, 8, 15),
                LocalDateTime.of(2026, 10, 3, 10, 45)
        );
        assertEquals(150, minutes);
    }

    @Test
    void updateTimeLog_WhenTechnicianEditsOwnLog_ShouldRecalculateDuration() {
        TimeLog existing = savedLog(80L, technicianA, 60);
        UpdateTimeLogRequest request = UpdateTimeLogRequest.builder()
                .startTime(LocalDateTime.of(2026, 10, 3, 9, 0))
                .endTime(LocalDateTime.of(2026, 10, 3, 12, 0))
                .notes("Extended")
                .build();
        when(workOrderAccessGuard.requireCurrentUser(TECH_A_EMAIL)).thenReturn(technicianA);
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, TECH_A_EMAIL)).thenReturn(assignedToA);
        when(workOrderAccessGuard.isTechnician(technicianA)).thenReturn(true);
        when(timeLogRepository.findByIdAndWorkOrderIdWithRelations(80L, 7L)).thenReturn(Optional.of(existing));
        when(timeLogRepository.existsOverlappingLog(eq(25L), any(), any(), eq(80L))).thenReturn(false);
        when(timeLogRepository.save(any(TimeLog.class))).thenReturn(existing);

        TimeLogResponse response = timeLogService.updateTimeLog(7L, 80L, request, TECH_A_EMAIL);

        assertEquals(180, existing.getDurationMinutes());
        assertEquals(180, response.getDurationMinutes());
    }

    private CreateTimeLogRequest validRequest() {
        return CreateTimeLogRequest.builder()
                .startTime(LocalDateTime.of(2026, 10, 3, 9, 0))
                .endTime(LocalDateTime.of(2026, 10, 3, 10, 0))
                .notes("On site")
                .build();
    }

    private TimeLog savedLog(Long id, User technician, int duration) {
        return TimeLog.builder()
                .id(id)
                .workOrder(assignedToA)
                .technician(technician)
                .startTime(LocalDateTime.of(2026, 10, 3, 9, 0))
                .endTime(LocalDateTime.of(2026, 10, 3, 11, 30))
                .durationMinutes(duration)
                .notes("On site")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
