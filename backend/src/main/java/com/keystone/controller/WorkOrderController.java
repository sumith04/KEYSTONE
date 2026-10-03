package com.keystone.controller;

import com.keystone.dto.AssignWorkOrderRequest;
import com.keystone.dto.CreateWorkOrderRequest;
import com.keystone.dto.UpdateWorkOrderRequest;
import com.keystone.dto.UserResponse;
import com.keystone.dto.WorkOrderPageResponse;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.service.WorkOrderService;
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
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_WORK_ORDER')")
    public ResponseEntity<WorkOrderPageResponse> getWorkOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) WorkOrderPriority priority,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long technicianId) {

        return ResponseEntity.ok(workOrderService.getWorkOrders(
                page, size, sort, search, status, priority, customerId, siteId, technicianId));
    }

    @GetMapping("/technicians")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_WORK_ORDER') or @authorizationService.hasPermission(authentication, 'ASSIGN_WORK_ORDER')")
    public ResponseEntity<List<UserResponse>> getAssignableTechnicians() {
        return ResponseEntity.ok(workOrderService.getAssignableTechnicians());
    }

    @GetMapping("/number/{workOrderNumber}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> getWorkOrderByNumber(@PathVariable String workOrderNumber) {
        return ResponseEntity.ok(workOrderService.getWorkOrderByNumber(workOrderNumber));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> getWorkOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.getWorkOrderById(id));
    }

    @PostMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'CREATE_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> createWorkOrder(
            @Valid @RequestBody CreateWorkOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workOrderService.createWorkOrder(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> updateWorkOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkOrderRequest request) {
        return ResponseEntity.ok(workOrderService.updateWorkOrder(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'DELETE_WORK_ORDER')")
    public ResponseEntity<Void> deleteWorkOrder(@PathVariable Long id) {
        workOrderService.deleteWorkOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'ASSIGN_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> assignWorkOrder(
            @PathVariable Long id,
            @Valid @RequestBody AssignWorkOrderRequest request) {
        return ResponseEntity.ok(workOrderService.assignWorkOrder(id, request));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'START_WORK')")
    public ResponseEntity<WorkOrderResponse> startWorkOrder(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.startWorkOrder(id));
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'HOLD_WORK')")
    public ResponseEntity<WorkOrderResponse> holdWorkOrder(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.holdWorkOrder(id));
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'RESUME_WORK')")
    public ResponseEntity<WorkOrderResponse> resumeWorkOrder(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.resumeWorkOrder(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'COMPLETE_WORK')")
    public ResponseEntity<WorkOrderResponse> completeWorkOrder(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.completeWorkOrder(id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'CLOSE_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> closeWorkOrder(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.closeWorkOrder(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'CANCEL_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> cancelWorkOrder(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.cancelWorkOrder(id));
    }
}
