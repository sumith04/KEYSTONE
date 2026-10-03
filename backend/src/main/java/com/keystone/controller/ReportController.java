package com.keystone.controller;

import com.keystone.dto.InventoryReportResponse;
import com.keystone.dto.SlaReportResponse;
import com.keystone.dto.TechnicianPerformanceResponse;
import com.keystone.dto.TimeReportResponse;
import com.keystone.dto.WorkOrderReportResponse;
import com.keystone.service.ReportService;
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
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/work-orders")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_REPORT')")
    public ResponseEntity<WorkOrderReportResponse> getWorkOrderReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.getWorkOrderReport(from, to, userDetails.getUsername()));
    }

    @GetMapping("/sla")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_REPORT')")
    public ResponseEntity<SlaReportResponse> getSlaReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.getSlaReport(from, to, userDetails.getUsername()));
    }

    @GetMapping("/technician-performance")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_REPORT')")
    public ResponseEntity<List<TechnicianPerformanceResponse>> getTechnicianPerformance(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.getTechnicianPerformance(from, to, userDetails.getUsername()));
    }

    @GetMapping("/inventory")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_REPORT')")
    public ResponseEntity<InventoryReportResponse> getInventoryReport(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.getInventoryReport(userDetails.getUsername()));
    }

    @GetMapping("/time")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_REPORT')")
    public ResponseEntity<TimeReportResponse> getTimeReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.getTimeReport(from, to, userDetails.getUsername()));
    }
}
