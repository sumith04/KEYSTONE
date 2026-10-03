package com.keystone.controller;

import com.keystone.dto.CreateServiceRequest;
import com.keystone.dto.CustomerPortalSummaryResponse;
import com.keystone.dto.CustomerProfileResponse;
import com.keystone.dto.CustomerWorkOrderPageResponse;
import com.keystone.dto.CustomerWorkOrderResponse;
import com.keystone.dto.ServiceRequestPageResponse;
import com.keystone.dto.ServiceRequestResponse;
import com.keystone.dto.SitePageResponse;
import com.keystone.dto.UpdateServiceRequest;
import com.keystone.enums.ServiceRequestStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.service.CustomerPortalService;
import com.keystone.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerPortalController {

    private final CustomerPortalService customerPortalService;
    private final ServiceRequestService serviceRequestService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(customerPortalService.getProfile(userDetails.getUsername()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerPortalSummaryResponse> getSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(customerPortalService.getSummary(userDetails.getUsername()));
    }

    @GetMapping("/sites")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SitePageResponse> getSites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(customerPortalService.getSites(page, size, userDetails.getUsername()));
    }

    @GetMapping("/requests")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_OWN_REQUEST')")
    public ResponseEntity<ServiceRequestPageResponse> getRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ServiceRequestStatus status,
            @RequestParam(required = false) WorkOrderPriority priority,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serviceRequestService.getMyRequests(
                page, size, sort, search, status, priority, userDetails.getUsername()));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_OWN_REQUEST')")
    public ResponseEntity<ServiceRequestResponse> getRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serviceRequestService.getMyRequest(id, userDetails.getUsername()));
    }

    @PostMapping("/requests")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'REQUEST_RAISE')")
    public ResponseEntity<ServiceRequestResponse> createRequest(
            @Valid @RequestBody CreateServiceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceRequestService.createMyRequest(request, userDetails.getUsername()));
    }

    @PutMapping("/requests/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'REQUEST_RAISE')")
    public ResponseEntity<ServiceRequestResponse> updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serviceRequestService.updateMyRequest(id, request, userDetails.getUsername()));
    }

    @PatchMapping("/requests/{id}/cancel")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'REQUEST_RAISE')")
    public ResponseEntity<ServiceRequestResponse> cancelRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serviceRequestService.cancelMyRequest(id, userDetails.getUsername()));
    }

    @GetMapping("/work-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerWorkOrderPageResponse> getWorkOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) WorkOrderStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(customerPortalService.getWorkOrders(
                page, size, sort, search, status, userDetails.getUsername()));
    }

    @GetMapping("/work-orders/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerWorkOrderResponse> getWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(customerPortalService.getWorkOrder(id, userDetails.getUsername()));
    }
}
