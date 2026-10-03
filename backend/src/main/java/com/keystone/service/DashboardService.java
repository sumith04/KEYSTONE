package com.keystone.service;

import com.keystone.dto.DashboardSummaryResponse;
import com.keystone.dto.RecentWorkOrderResponse;
import com.keystone.dto.WorkOrderTrendResponse;

import java.util.List;

public interface DashboardService {

    DashboardSummaryResponse getSummary(String from, String to, String currentUsername);

    List<RecentWorkOrderResponse> getRecentWorkOrders(String from, String to, Integer limit, String currentUsername);

    List<WorkOrderTrendResponse> getWorkOrderTrend(String from, String to, String interval, String currentUsername);
}
