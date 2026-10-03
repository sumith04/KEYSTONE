package com.keystone.controller;

import com.keystone.dto.AssignWorkOrderRequest;
import com.keystone.dto.CreateWorkOrderRequest;
import com.keystone.dto.UpdateWorkOrderRequest;
import com.keystone.dto.UserResponse;
import com.keystone.dto.WorkOrderPageResponse;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.dto.WorkOrderSlaPageResponse;
import com.keystone.dto.WorkOrderSlaResponse;
import com.keystone.dto.WorkOrderSummaryResponse;
import com.keystone.enums.SlaStatus;
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
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) SlaStatus slaStatus,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(workOrderService.getWorkOrders(
                page, size, sort, search, status, priority, customerId, siteId, technicianId, slaStatus,
                userDetails.getUsername()));
    }

    @GetMapping("/summary")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_WORK_ORDER')")
    public ResponseEntity<WorkOrderSummaryResponse> getWorkOrderSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.getWorkOrderSummary(userDetails.getUsername()));
    }

    @GetMapping("/sla")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_WORK_ORDER')")
    public ResponseEntity<WorkOrderSlaPageResponse> getWorkOrderSlaList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) WorkOrderPriority priority,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) SlaStatus slaStatus,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.getWorkOrderSlaList(
                page, size, sort, search, status, priority, customerId, siteId, technicianId, slaStatus,
                userDetails.getUsername()));
    }

    @GetMapping("/{id}/sla")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_WORK_ORDER')")
    public ResponseEntity<WorkOrderSlaResponse> getWorkOrderSla(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.getWorkOrderSla(id, userDetails.getUsername()));
    }

    @GetMapping("/technicians")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_WORK_ORDER') or @authorizationService.hasPermission(authentication, 'ASSIGN_WORK_ORDER')")
    public ResponseEntity<List<UserResponse>> getAssignableTechnicians() {
        return ResponseEntity.ok(workOrderService.getAssignableTechnicians());
    }

    @GetMapping("/number/{workOrderNumber}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> getWorkOrderByNumber(
            @PathVariable String workOrderNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.getWorkOrderByNumber(workOrderNumber, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> getWorkOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.getWorkOrderById(id, userDetails.getUsername()));
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
            @Valid @RequestBody UpdateWorkOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.updateWorkOrder(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'DELETE_WORK_ORDER')")
    public ResponseEntity<Void> deleteWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        workOrderService.deleteWorkOrder(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'ASSIGN_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> assignWorkOrder(
            @PathVariable Long id,
            @Valid @RequestBody AssignWorkOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.assignWorkOrder(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'START_WORK')")
    public ResponseEntity<WorkOrderResponse> startWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.startWorkOrder(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'HOLD_WORK')")
    public ResponseEntity<WorkOrderResponse> holdWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.holdWorkOrder(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'RESUME_WORK')")
    public ResponseEntity<WorkOrderResponse> resumeWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.resumeWorkOrder(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'COMPLETE_WORK')")
    public ResponseEntity<WorkOrderResponse> completeWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.completeWorkOrder(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'CLOSE_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> closeWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.closeWorkOrder(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'CANCEL_WORK_ORDER')")
    public ResponseEntity<WorkOrderResponse> cancelWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(workOrderService.cancelWorkOrder(id, userDetails.getUsername()));
    }
}
