package com.keystone.controller;

import com.keystone.dto.DashboardSummaryResponse;
import com.keystone.dto.RecentWorkOrderResponse;
import com.keystone.dto.WorkOrderTrendResponse;
import com.keystone.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_DASHBOARD')")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getSummary(from, to, userDetails.getUsername()));
    }

    @GetMapping("/recent-work-orders")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_DASHBOARD')")
    public ResponseEntity<List<RecentWorkOrderResponse>> getRecentWorkOrders(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getRecentWorkOrders(from, to, limit, userDetails.getUsername()));
    }

    @GetMapping("/work-order-trend")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_DASHBOARD')")
    public ResponseEntity<List<WorkOrderTrendResponse>> getWorkOrderTrend(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String interval,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getWorkOrderTrend(from, to, interval, userDetails.getUsername()));
    }
}
