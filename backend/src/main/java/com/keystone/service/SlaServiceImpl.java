package com.keystone.service;

import com.keystone.dto.CreateSlaPolicyRequest;
import com.keystone.dto.SlaPolicyPageResponse;
import com.keystone.dto.SlaPolicyResponse;
import com.keystone.dto.UpdateSlaPolicyRequest;
import com.keystone.dto.WorkOrderSlaPageResponse;
import com.keystone.dto.WorkOrderSlaResponse;
import com.keystone.entity.Customer;
import com.keystone.entity.SlaPolicy;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.Role;
import com.keystone.enums.SlaStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SlaPolicyRepository;
import com.keystone.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SlaServiceImpl implements SlaService {

    private static final Set<String> POLICY_SORTABLE_FIELDS = Set.of(
            "name", "priority", "responseTimeMinutes", "resolutionTimeMinutes", "active", "createdAt", "updatedAt"
    );

    private static final Set<String> WORK_ORDER_SORTABLE_FIELDS = Set.of(
            "createdAt", "updatedAt", "workOrderNumber", "title", "status", "priority",
            "workType", "scheduledStart", "scheduledEnd"
    );

    private final SlaPolicyRepository slaPolicyRepository;
    private final CustomerRepository customerRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderAccessGuard workOrderAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public SlaPolicyPageResponse getPolicies(
            int page,
            int size,
            String sort,
            String search,
            WorkOrderPriority priority,
            Boolean active) {

        Pageable pageable = buildPageable(page, size, sort, POLICY_SORTABLE_FIELDS, "name", Sort.Direction.ASC);
        Page<SlaPolicy> policyPage = slaPolicyRepository.searchPolicies(normalizeSearch(search), priority, active, pageable);

        return SlaPolicyPageResponse.builder()
                .content(policyPage.getContent().stream().map(SlaPolicyResponse::fromEntity).toList())
                .page(policyPage.getNumber())
                .size(policyPage.getSize())
                .totalElements(policyPage.getTotalElements())
                .totalPages(policyPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SlaPolicyResponse getPolicyById(Long id) {
        return SlaPolicyResponse.fromEntity(findPolicy(id));
    }

    @Override
    @Transactional
    public SlaPolicyResponse createPolicy(CreateSlaPolicyRequest request) {
        assertDurations(request.getResponseTimeMinutes(), request.getResolutionTimeMinutes());
        String name = trimToNull(request.getName());
        assertNameAvailable(name, null);

        SlaPolicy policy = SlaPolicy.builder()
                .name(name)
                .description(trimToNull(request.getDescription()))
                .priority(request.getPriority())
                .responseTimeMinutes(request.getResponseTimeMinutes())
                .resolutionTimeMinutes(request.getResolutionTimeMinutes())
                .active(request.getActive() == null || request.getActive())
                .build();

        return SlaPolicyResponse.fromEntity(slaPolicyRepository.save(policy));
    }

    @Override
    @Transactional
    public SlaPolicyResponse updatePolicy(Long id, UpdateSlaPolicyRequest request) {
        assertDurations(request.getResponseTimeMinutes(), request.getResolutionTimeMinutes());
        SlaPolicy policy = findPolicy(id);
        String name = trimToNull(request.getName());
        assertNameAvailable(name, id);

        policy.setName(name);
        policy.setDescription(trimToNull(request.getDescription()));
        policy.setPriority(request.getPriority());
        policy.setResponseTimeMinutes(request.getResponseTimeMinutes());
        policy.setResolutionTimeMinutes(request.getResolutionTimeMinutes());
        policy.setActive(Boolean.TRUE.equals(request.getActive()));

        return SlaPolicyResponse.fromEntity(slaPolicyRepository.save(policy));
    }

    @Override
    @Transactional
    public void deletePolicy(Long id) {
        SlaPolicy policy = findPolicy(id);
        if (customerRepository.existsBySlaPolicyId(id)) {
            throw new ApiException(
                    "Cannot delete SLA policy because one or more customers are associated with it.",
                    HttpStatus.CONFLICT
            );
        }
        if (workOrderRepository.existsBySlaPolicyId(id)) {
            throw new ApiException(
                    "Cannot delete SLA policy because it is referenced by historical work orders.",
                    HttpStatus.CONFLICT
            );
        }
        slaPolicyRepository.delete(policy);
    }

    @Override
    public void applySnapshot(WorkOrder workOrder, Customer customer) {
        if (workOrder == null || customer == null) {
            return;
        }
        SlaPolicy policy = customer.getSlaPolicy();
        if (policy == null || !policy.isActive()) {
            return;
        }

        LocalDateTime createdAt = workOrder.getCreatedAt() != null ? workOrder.getCreatedAt() : LocalDateTime.now();
        workOrder.setSlaPolicy(policy);
        workOrder.setSlaPolicyName(policy.getName());
        workOrder.setSlaResponseDueAt(createdAt.plusMinutes(policy.getResponseTimeMinutes()));
        workOrder.setSlaResolutionDueAt(createdAt.plusMinutes(policy.getResolutionTimeMinutes()));
    }

    @Override
    public void recordResponseIfNeeded(WorkOrder workOrder, LocalDateTime at) {
        if (workOrder == null || workOrder.getResponseAt() != null) {
            return;
        }
        LocalDateTime responseAt = at != null ? at : LocalDateTime.now();
        workOrder.setResponseAt(responseAt);
        if (workOrder.getSlaResponseDueAt() != null) {
            workOrder.setResponseBreached(responseAt.isAfter(workOrder.getSlaResponseDueAt()));
        }
    }

    @Override
    public void recordResolutionIfNeeded(WorkOrder workOrder, LocalDateTime at) {
        if (workOrder == null || workOrder.getResolvedAt() != null) {
            return;
        }
        LocalDateTime resolvedAt = at != null ? at : LocalDateTime.now();
        workOrder.setResolvedAt(resolvedAt);
        if (workOrder.getSlaResolutionDueAt() != null) {
            workOrder.setResolutionBreached(resolvedAt.isAfter(workOrder.getSlaResolutionDueAt()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderSlaResponse getWorkOrderSla(Long workOrderId, String currentUsername) {
        WorkOrder workOrder = workOrderAccessGuard.requireAccessibleWorkOrder(workOrderId, currentUsername);
        return SlaCalculator.toResponse(workOrder, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderSlaPageResponse getWorkOrderSlaList(
            int page,
            int size,
            String sort,
            String search,
            WorkOrderStatus status,
            WorkOrderPriority priority,
            Long customerId,
            Long siteId,
            Long technicianId,
            SlaStatus slaStatus,
            String currentUsername) {

        User currentUser = workOrderAccessGuard.requireCurrentUser(currentUsername);
        Long scopedTechnicianId = currentUser.getRole() == Role.TECHNICIAN ? currentUser.getId() : technicianId;
        Pageable pageable = buildPageable(page, size, sort, WORK_ORDER_SORTABLE_FIELDS, "createdAt", Sort.Direction.DESC);
        LocalDateTime now = LocalDateTime.now();

        /*
         * slaStatus is calculated, not stored. When the filter is absent we reuse the existing
         * paginated Work Order query and evaluate status only for the current page. When the
         * filter is present we load the matching scoped result set, filter in memory, then slice.
         * This avoids a second repository and vendor-specific datetime SQL.
         */
        if (slaStatus == null) {
            Page<WorkOrder> workOrderPage = workOrderRepository.searchWorkOrders(
                    normalizeSearch(search), status, priority, customerId, siteId, scopedTechnicianId, pageable);
            List<WorkOrderSlaResponse> content = workOrderPage.getContent()
                    .stream()
                    .map(workOrder -> SlaCalculator.toResponse(workOrder, now))
                    .toList();
            return WorkOrderSlaPageResponse.builder()
                    .content(content)
                    .page(workOrderPage.getNumber())
                    .size(workOrderPage.getSize())
                    .totalElements(workOrderPage.getTotalElements())
                    .totalPages(workOrderPage.getTotalPages())
                    .build();
        }

        List<WorkOrderSlaResponse> filtered = workOrderRepository.searchWorkOrders(
                        normalizeSearch(search),
                        status,
                        priority,
                        customerId,
                        siteId,
                        scopedTechnicianId,
                        Pageable.unpaged())
                .stream()
                .map(workOrder -> SlaCalculator.toResponse(workOrder, now))
                .filter(item -> slaStatus == item.getSlaStatus())
                .toList();

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int from = Math.min(pageNumber * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) filtered.size() / pageSize);

        return WorkOrderSlaPageResponse.builder()
                .content(filtered.subList(from, to))
                .page(pageNumber)
                .size(pageSize)
                .totalElements(filtered.size())
                .totalPages(totalPages)
                .build();
    }

    private SlaPolicy findPolicy(Long id) {
        return slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy not found with id: " + id));
    }

    private void assertNameAvailable(String name, Long currentId) {
        boolean exists = currentId == null
                ? slaPolicyRepository.existsByNameIgnoreCase(name)
                : slaPolicyRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);
        if (exists) {
            throw new DuplicateResourceException("An SLA policy named " + name + " already exists.");
        }
    }

    private void assertDurations(Integer responseTimeMinutes, Integer resolutionTimeMinutes) {
        if (responseTimeMinutes == null || responseTimeMinutes <= 0) {
            throw new ApiException("Response time must be greater than 0.", HttpStatus.BAD_REQUEST);
        }
        if (resolutionTimeMinutes == null || resolutionTimeMinutes <= 0) {
            throw new ApiException("Resolution time must be greater than 0.", HttpStatus.BAD_REQUEST);
        }
        if (resolutionTimeMinutes < responseTimeMinutes) {
            throw new ApiException("Resolution time must be greater than or equal to response time.", HttpStatus.BAD_REQUEST);
        }
    }

    private Pageable buildPageable(
            int page,
            int size,
            String sort,
            Set<String> sortableFields,
            String defaultProperty,
            Sort.Direction defaultDirection) {

        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = defaultDirection;
        String property = defaultProperty;

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String candidate = parts[0].trim();
            if (sortableFields.contains(candidate)) {
                property = candidate;
            }
            if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.DESC;
            } else if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.ASC;
            }
        }

        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, property));
    }

    private String normalizeSearch(String search) {
        return (search != null && !search.isBlank()) ? search.trim() : null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
