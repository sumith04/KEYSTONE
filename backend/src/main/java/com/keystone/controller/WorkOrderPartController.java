package com.keystone.controller;

import com.keystone.dto.CreateWorkOrderPartRequest;
import com.keystone.dto.UpdateWorkOrderPartRequest;
import com.keystone.dto.WorkOrderPartResponse;
import com.keystone.service.WorkOrderPartService;
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
@RequestMapping("/api/work-orders/{workOrderId}/parts")
@RequiredArgsConstructor
public class WorkOrderPartController {

    private final WorkOrderPartService workOrderPartService;

    @GetMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_PART')")
    public ResponseEntity<List<WorkOrderPartResponse>> getWorkOrderParts(
            @PathVariable Long workOrderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderPartService.getWorkOrderParts(workOrderId, userDetails.getUsername()));
    }

    @PostMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'USE_PARTS')")
    public ResponseEntity<WorkOrderPartResponse> addWorkOrderPart(
            @PathVariable Long workOrderId,
            @Valid @RequestBody CreateWorkOrderPartRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workOrderPartService.addWorkOrderPart(workOrderId, request, userDetails.getUsername()));
    }

    @PutMapping("/{workOrderPartId}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'USE_PARTS')")
    public ResponseEntity<WorkOrderPartResponse> updateWorkOrderPart(
            @PathVariable Long workOrderId,
            @PathVariable Long workOrderPartId,
            @Valid @RequestBody UpdateWorkOrderPartRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderPartService.updateWorkOrderPart(
                workOrderId, workOrderPartId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{workOrderPartId}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'USE_PARTS')")
    public ResponseEntity<Void> deleteWorkOrderPart(
            @PathVariable Long workOrderId,
            @PathVariable Long workOrderPartId,
            @AuthenticationPrincipal UserDetails userDetails) {
        workOrderPartService.deleteWorkOrderPart(workOrderId, workOrderPartId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
