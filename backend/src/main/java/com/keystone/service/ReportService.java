package com.keystone.service;

import com.keystone.dto.InventoryReportResponse;
import com.keystone.dto.SlaReportResponse;
import com.keystone.dto.TechnicianPerformanceResponse;
import com.keystone.dto.TimeReportResponse;
import com.keystone.dto.WorkOrderReportResponse;

import java.util.List;

public interface ReportService {

    WorkOrderReportResponse getWorkOrderReport(String from, String to, String currentUsername);

    SlaReportResponse getSlaReport(String from, String to, String currentUsername);

    List<TechnicianPerformanceResponse> getTechnicianPerformance(String from, String to, String currentUsername);

    InventoryReportResponse getInventoryReport(String currentUsername);

    TimeReportResponse getTimeReport(String from, String to, String currentUsername);
}
