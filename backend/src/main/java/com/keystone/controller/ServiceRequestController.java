package com.keystone.controller;

import com.keystone.dto.ServiceRequestPageResponse;
import com.keystone.dto.ServiceRequestResponse;
import com.keystone.enums.ServiceRequestStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @GetMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_SERVICE_REQUEST')")
    public ResponseEntity<ServiceRequestPageResponse> getRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ServiceRequestStatus status,
            @RequestParam(required = false) WorkOrderPriority priority,
            @RequestParam(required = false) Long customerId) {
        return ResponseEntity.ok(serviceRequestService.getRequests(
                page, size, sort, search, status, priority, customerId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_SERVICE_REQUEST')")
    public ResponseEntity<ServiceRequestResponse> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.getRequest(id));
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_SERVICE_REQUEST')")
    public ResponseEntity<ServiceRequestResponse> acknowledge(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serviceRequestService.acknowledge(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_SERVICE_REQUEST')")
    public ResponseEntity<ServiceRequestResponse> markInReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serviceRequestService.markInReview(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_SERVICE_REQUEST')")
    public ResponseEntity<ServiceRequestResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serviceRequestService.reject(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/convert-to-work-order")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'CONVERT_SERVICE_REQUEST')")
    public ResponseEntity<ServiceRequestResponse> convertToWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serviceRequestService.convertToWorkOrder(id, userDetails.getUsername()));
    }
}
