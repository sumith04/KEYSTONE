package com.keystone.service;

import com.keystone.dto.CreateTimeLogRequest;
import com.keystone.dto.TimeLogResponse;
import com.keystone.dto.UpdateTimeLogRequest;
import com.keystone.entity.TimeLog;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.TimeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeLogServiceImpl implements TimeLogService {

    static final int MAX_DURATION_MINUTES = 7 * 24 * 60;

    private final TimeLogRepository timeLogRepository;
    private final WorkOrderAccessGuard workOrderAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public List<TimeLogResponse> getTimeLogs(Long workOrderId, String currentUsername) {
        workOrderAccessGuard.requireAccessibleWorkOrder(workOrderId, currentUsername);
        return timeLogRepository.findByWorkOrderIdWithRelations(workOrderId)
                .stream()
                .map(TimeLogResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public TimeLogResponse addTimeLog(Long workOrderId, CreateTimeLogRequest request, String currentUsername) {
        User currentUser = workOrderAccessGuard.requireCurrentUser(currentUsername);
        WorkOrder workOrder = workOrderAccessGuard.requireAccessibleWorkOrder(workOrderId, currentUsername);
        workOrderAccessGuard.assertTimeLogsMutable(workOrder);

        int durationMinutes = calculateDurationMinutes(request.getStartTime(), request.getEndTime());
        assertNoOverlap(currentUser.getId(), request.getStartTime(), request.getEndTime(), null);

        TimeLog timeLog = TimeLog.builder()
                .workOrder(workOrder)
                .technician(currentUser)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationMinutes(durationMinutes)
                .notes(trimToNull(request.getNotes()))
                .build();

        TimeLog saved = timeLogRepository.save(timeLog);
        return TimeLogResponse.fromEntity(
                timeLogRepository.findByIdAndWorkOrderIdWithRelations(saved.getId(), workOrderId)
                        .orElse(saved)
        );
    }

    @Override
    @Transactional
    public TimeLogResponse updateTimeLog(
            Long workOrderId,
            Long timeLogId,
            UpdateTimeLogRequest request,
            String currentUsername) {

        User currentUser = workOrderAccessGuard.requireCurrentUser(currentUsername);
        WorkOrder workOrder = workOrderAccessGuard.requireAccessibleWorkOrder(workOrderId, currentUsername);
        workOrderAccessGuard.assertTimeLogsMutable(workOrder);

        TimeLog timeLog = findTimeLog(workOrderId, timeLogId);
        assertCanModifyTimeLog(currentUser, timeLog);

        int durationMinutes = calculateDurationMinutes(request.getStartTime(), request.getEndTime());
        assertNoOverlap(timeLog.getTechnician().getId(), request.getStartTime(), request.getEndTime(), timeLog.getId());

        timeLog.setStartTime(request.getStartTime());
        timeLog.setEndTime(request.getEndTime());
        timeLog.setDurationMinutes(durationMinutes);
        timeLog.setNotes(trimToNull(request.getNotes()));

        timeLogRepository.save(timeLog);
        return TimeLogResponse.fromEntity(
                timeLogRepository.findByIdAndWorkOrderIdWithRelations(timeLogId, workOrderId)
                        .orElse(timeLog)
        );
    }

    @Override
    @Transactional
    public void deleteTimeLog(Long workOrderId, Long timeLogId, String currentUsername) {
        User currentUser = workOrderAccessGuard.requireCurrentUser(currentUsername);
        WorkOrder workOrder = workOrderAccessGuard.requireAccessibleWorkOrder(workOrderId, currentUsername);
        workOrderAccessGuard.assertTimeLogsMutable(workOrder);

        TimeLog timeLog = findTimeLog(workOrderId, timeLogId);
        assertCanModifyTimeLog(currentUser, timeLog);
        timeLogRepository.delete(timeLog);
    }

    int calculateDurationMinutes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new ApiException("Start time and end time are required.", HttpStatus.BAD_REQUEST);
        }
        if (!endTime.isAfter(startTime)) {
            throw new ApiException("End time must be after start time.", HttpStatus.BAD_REQUEST);
        }

        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            throw new ApiException("Duration must be greater than 0 minutes.", HttpStatus.BAD_REQUEST);
        }
        if (minutes > MAX_DURATION_MINUTES) {
            throw new ApiException(
                    "Duration cannot exceed " + MAX_DURATION_MINUTES + " minutes.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return (int) minutes;
    }

    private void assertNoOverlap(Long technicianId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
        if (timeLogRepository.existsOverlappingLog(technicianId, startTime, endTime, excludeId)) {
            throw new ApiException(
                    "This time log overlaps an existing log for the same technician.",
                    HttpStatus.CONFLICT
            );
        }
    }

    private void assertCanModifyTimeLog(User currentUser, TimeLog timeLog) {
        if (!workOrderAccessGuard.isTechnician(currentUser)) {
            return;
        }
        if (timeLog.getTechnician() == null || !currentUser.getId().equals(timeLog.getTechnician().getId())) {
            throw new ApiException("Technicians can only modify their own time logs.", HttpStatus.FORBIDDEN);
        }
    }

    private TimeLog findTimeLog(Long workOrderId, Long timeLogId) {
        return timeLogRepository.findByIdAndWorkOrderIdWithRelations(timeLogId, workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Time log not found with id: " + timeLogId));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
