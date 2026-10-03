package com.keystone.service;

import com.keystone.dto.CustomerPortalSummaryResponse;
import com.keystone.dto.CustomerProfileResponse;
import com.keystone.dto.CustomerWorkOrderPageResponse;
import com.keystone.dto.CustomerWorkOrderResponse;
import com.keystone.dto.SitePageResponse;
import com.keystone.enums.SlaStatus;
import com.keystone.enums.WorkOrderStatus;

public interface CustomerPortalService {

    CustomerProfileResponse getProfile(String currentUsername);

    SitePageResponse getSites(int page, int size, String currentUsername);

    CustomerPortalSummaryResponse getSummary(String currentUsername);

    CustomerWorkOrderPageResponse getWorkOrders(
            int page,
            int size,
            String sort,
            String search,
            WorkOrderStatus status,
            String currentUsername
    );

    CustomerWorkOrderResponse getWorkOrder(Long id, String currentUsername);

    long countSlaWarnings(String currentUsername);
}
