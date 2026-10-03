package com.keystone.controller;

import com.keystone.dto.CreateTimeLogRequest;
import com.keystone.dto.TimeLogResponse;
import com.keystone.dto.UpdateTimeLogRequest;
import com.keystone.service.TimeLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders/{workOrderId}/time-logs")
@RequiredArgsConstructor
public class TimeLogController {

    private final TimeLogService timeLogService;

    @GetMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_TIME_LOGS')")
    public ResponseEntity<List<TimeLogResponse>> getTimeLogs(
            @PathVariable Long workOrderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(timeLogService.getTimeLogs(workOrderId, userDetails.getUsername()));
    }

    @PostMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'ADD_TIME_LOG')")
    public ResponseEntity<TimeLogResponse> addTimeLog(
            @PathVariable Long workOrderId,
            @Valid @RequestBody CreateTimeLogRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timeLogService.addTimeLog(workOrderId, request, userDetails.getUsername()));
    }

    @PutMapping("/{timeLogId}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'ADD_TIME_LOG')")
    public ResponseEntity<TimeLogResponse> updateTimeLog(
            @PathVariable Long workOrderId,
            @PathVariable Long timeLogId,
            @Valid @RequestBody UpdateTimeLogRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(timeLogService.updateTimeLog(
                workOrderId, timeLogId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{timeLogId}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'ADD_TIME_LOG')")
    public ResponseEntity<Void> deleteTimeLog(
            @PathVariable Long workOrderId,
            @PathVariable Long timeLogId,
            @AuthenticationPrincipal UserDetails userDetails) {
        timeLogService.deleteTimeLog(workOrderId, timeLogId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
