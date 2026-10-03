package com.keystone.service;

import com.keystone.dto.CustomerPortalSummaryResponse;
import com.keystone.dto.CustomerProfileResponse;
import com.keystone.dto.CustomerWorkOrderPageResponse;
import com.keystone.dto.CustomerWorkOrderResponse;
import com.keystone.dto.SitePageResponse;
import com.keystone.dto.SiteResponse;
import com.keystone.entity.Customer;
import com.keystone.entity.ServiceRequest;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.ServiceRequestStatus;
import com.keystone.enums.SlaStatus;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.ServiceRequestRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerPortalServiceImpl implements CustomerPortalService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "createdAt", "updatedAt", "workOrderNumber", "title", "status", "priority"
    );

    private static final Set<WorkOrderStatus> ACTIVE_STATUSES = EnumSet.of(
            WorkOrderStatus.NEW,
            WorkOrderStatus.ASSIGNED,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.ON_HOLD
    );

    private final CustomerAccessGuard customerAccessGuard;
    private final SiteRepository siteRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(String currentUsername) {
        return CustomerProfileResponse.fromEntity(customerAccessGuard.requireLinkedCustomer(currentUsername));
    }

    @Override
    @Transactional(readOnly = true)
    public SitePageResponse getSites(int page, int size, String currentUsername) {
        Customer customer = customerAccessGuard.requireLinkedCustomer(currentUsername);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("siteName"));
        Page<com.keystone.entity.Site> result = siteRepository.searchSites(null, null, customer.getId(), pageable);
        return SitePageResponse.builder()
                .content(result.getContent().stream().map(SiteResponse::fromEntity).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerPortalSummaryResponse getSummary(String currentUsername) {
        Customer customer = customerAccessGuard.requireLinkedCustomer(currentUsername);
        long submitted = serviceRequestRepository.countByCustomerIdAndStatus(customer.getId(), ServiceRequestStatus.SUBMITTED);
        long acknowledged = serviceRequestRepository.countByCustomerIdAndStatus(customer.getId(), ServiceRequestStatus.ACKNOWLEDGED);
        long inReview = serviceRequestRepository.countByCustomerIdAndStatus(customer.getId(), ServiceRequestStatus.IN_REVIEW);
        long activeWorkOrders = ACTIVE_STATUSES.stream()
                .mapToLong(status -> workOrderRepository.countByCustomerIdAndStatus(customer.getId(), status))
                .sum();
        long completed = workOrderRepository.countByCustomerIdAndStatus(customer.getId(), WorkOrderStatus.COMPLETED)
                + workOrderRepository.countByCustomerIdAndStatus(customer.getId(), WorkOrderStatus.CLOSED);
        return CustomerPortalSummaryResponse.builder()
                .submittedRequests(submitted)
                .openRequests(submitted + acknowledged + inReview)
                .activeWorkOrders(activeWorkOrders)
                .completedWorkOrders(completed)
                .slaWarnings(countSlaWarnings(customer.getId()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerWorkOrderPageResponse getWorkOrders(
            int page,
            int size,
            String sort,
            String search,
            WorkOrderStatus status,
            String currentUsername) {

        Customer customer = customerAccessGuard.requireLinkedCustomer(currentUsername);
        Page<WorkOrder> result = workOrderRepository.searchWorkOrders(
                normalizeSearch(search), status, null, customer.getId(), null, null, buildPageable(page, size, sort));
        return CustomerWorkOrderPageResponse.builder()
                .content(result.getContent().stream().map(this::toCustomerWorkOrder).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerWorkOrderResponse getWorkOrder(Long id, String currentUsername) {
        Customer customer = customerAccessGuard.requireLinkedCustomer(currentUsername);
        WorkOrder workOrder = workOrderRepository.findByIdAndCustomerIdWithRelations(id, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with id: " + id));
        return toCustomerWorkOrder(workOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSlaWarnings(String currentUsername) {
        Customer customer = customerAccessGuard.requireLinkedCustomer(currentUsername);
        return countSlaWarnings(customer.getId());
    }

    private long countSlaWarnings(Long customerId) {
        LocalDateTime now = LocalDateTime.now();
        return workOrderRepository.searchWorkOrders(null, null, null, customerId, null, null, Pageable.unpaged())
                .stream()
                .map(workOrder -> SlaCalculator.calculateStatus(workOrder, now))
                .filter(status -> status == SlaStatus.AT_RISK || status == SlaStatus.BREACHED)
                .count();
    }

    private CustomerWorkOrderResponse toCustomerWorkOrder(WorkOrder workOrder) {
        ServiceRequest request = workOrder.getId() == null
                ? null
                : serviceRequestRepository.findByWorkOrderId(workOrder.getId()).orElse(null);
        return CustomerWorkOrderResponse.fromEntity(workOrder, request);
    }

    private Pageable buildPageable(int page, int size, String sort) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = Sort.Direction.DESC;
        String property = "createdAt";
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String candidate = parts[0].trim();
            if (SORTABLE_FIELDS.contains(candidate)) {
                property = candidate;
            }
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.ASC;
            }
        }
        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, property));
    }

    private String normalizeSearch(String search) {
        return (search != null && !search.isBlank()) ? search.trim() : null;
    }
}
