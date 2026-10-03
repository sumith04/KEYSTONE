package com.keystone.service;

import com.keystone.dto.AssignWorkOrderRequest;
import com.keystone.dto.CreateWorkOrderRequest;
import com.keystone.dto.UpdateWorkOrderRequest;
import com.keystone.dto.UserResponse;
import com.keystone.dto.WorkOrderPageResponse;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.dto.WorkOrderSlaPageResponse;
import com.keystone.dto.WorkOrderSlaResponse;
import com.keystone.dto.WorkOrderSummaryResponse;
import com.keystone.entity.Customer;
import com.keystone.entity.Site;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.Role;
import com.keystone.enums.SlaStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.repository.TimeLogRepository;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderPartRepository;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "createdAt", "updatedAt", "workOrderNumber", "title", "status", "priority",
            "workType", "scheduledStart", "scheduledEnd"
    );

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            WorkOrderStatus.NEW, EnumSet.of(WorkOrderStatus.ASSIGNED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.ASSIGNED, EnumSet.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.IN_PROGRESS, EnumSet.of(WorkOrderStatus.ON_HOLD, WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.ON_HOLD, EnumSet.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.COMPLETED, EnumSet.of(WorkOrderStatus.CLOSED),
            WorkOrderStatus.CLOSED, EnumSet.noneOf(WorkOrderStatus.class),
            WorkOrderStatus.CANCELLED, EnumSet.noneOf(WorkOrderStatus.class)
    );

    private static final Set<WorkOrderStatus> TERMINAL_UPDATE_STATUSES = EnumSet.of(
            WorkOrderStatus.COMPLETED, WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED
    );

    private static final Set<WorkOrderStatus> NON_DELETABLE_STATUSES = EnumSet.of(
            WorkOrderStatus.COMPLETED, WorkOrderStatus.CLOSED
    );

    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final WorkOrderPartRepository workOrderPartRepository;
    private final TimeLogRepository timeLogRepository;
    private final SlaService slaService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public WorkOrderPageResponse getWorkOrders(
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

        User currentUser = findCurrentUser(currentUsername);
        Long scopedTechnicianId = resolveScopedTechnicianId(currentUser, technicianId);

        Pageable pageable = buildPageable(page, size, sort);
        LocalDateTime now = LocalDateTime.now();

        if (slaStatus == null) {
            Page<WorkOrder> workOrderPage = workOrderRepository.searchWorkOrders(
                    normalizeSearch(search), status, priority, customerId, siteId, scopedTechnicianId, pageable);
            return toPageResponse(workOrderPage);
        }

        List<WorkOrder> filtered = workOrderRepository.searchWorkOrders(
                        normalizeSearch(search), status, priority, customerId, siteId, scopedTechnicianId, Pageable.unpaged())
                .stream()
                .filter(workOrder -> SlaCalculator.calculateStatus(workOrder, now) == slaStatus)
                .toList();

        int from = Math.min(pageable.getPageNumber() * pageable.getPageSize(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        int totalPages = pageable.getPageSize() == 0
                ? 0
                : (int) Math.ceil((double) filtered.size() / pageable.getPageSize());

        return WorkOrderPageResponse.builder()
                .content(filtered.subList(from, to).stream().map(WorkOrderResponse::fromEntity).toList())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(filtered.size())
                .totalPages(totalPages)
                .build();
    }

    private WorkOrderPageResponse toPageResponse(Page<WorkOrder> workOrderPage) {
        return WorkOrderPageResponse.builder()
                .content(workOrderPage.getContent().stream().map(WorkOrderResponse::fromEntity).toList())
                .page(workOrderPage.getNumber())
                .size(workOrderPage.getSize())
                .totalElements(workOrderPage.getTotalElements())
                .totalPages(workOrderPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderSummaryResponse getWorkOrderSummary(String currentUsername) {
        User currentUser = findCurrentUser(currentUsername);
        if (isTechnician(currentUser)) {
            Long technicianId = currentUser.getId();
            return WorkOrderSummaryResponse.builder()
                    .assigned(workOrderRepository.countByAssignedTechnicianIdAndStatus(technicianId, WorkOrderStatus.ASSIGNED))
                    .inProgress(workOrderRepository.countByAssignedTechnicianIdAndStatus(technicianId, WorkOrderStatus.IN_PROGRESS))
                    .onHold(workOrderRepository.countByAssignedTechnicianIdAndStatus(technicianId, WorkOrderStatus.ON_HOLD))
                    .completed(workOrderRepository.countByAssignedTechnicianIdAndStatus(technicianId, WorkOrderStatus.COMPLETED))
                    .build();
        }

        return WorkOrderSummaryResponse.builder()
                .assigned(workOrderRepository.countByStatus(WorkOrderStatus.ASSIGNED))
                .inProgress(workOrderRepository.countByStatus(WorkOrderStatus.IN_PROGRESS))
                .onHold(workOrderRepository.countByStatus(WorkOrderStatus.ON_HOLD))
                .completed(workOrderRepository.countByStatus(WorkOrderStatus.COMPLETED))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderResponse getWorkOrderById(Long id, String currentUsername) {
        return WorkOrderResponse.fromEntity(findAccessibleWorkOrder(id, currentUsername));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderResponse getWorkOrderByNumber(String workOrderNumber, String currentUsername) {
        String normalized = workOrderNumber == null ? null : workOrderNumber.trim().toUpperCase();
        WorkOrder workOrder = workOrderRepository.findByWorkOrderNumberWithRelations(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with number: " + workOrderNumber));
        assertTechnicianOwnsWorkOrder(workOrder, currentUsername, "Work order not found with number: " + workOrderNumber);
        return WorkOrderResponse.fromEntity(workOrder);
    }

    @Override
    @Transactional
    public WorkOrderResponse createWorkOrder(CreateWorkOrderRequest request, String currentUsername) {
        validateSchedule(request.getScheduledStart(), request.getScheduledEnd());

        Customer customer = findCustomer(request.getCustomerId());
        Site site = findSite(request.getSiteId());
        assertSiteBelongsToCustomer(site, customer.getId());
        User createdBy = findCurrentUser(currentUsername);

        WorkOrder workOrder = WorkOrder.builder()
                .title(trimToNull(request.getTitle()))
                .description(trimToNull(request.getDescription()))
                .customer(customer)
                .site(site)
                .priority(request.getPriority())
                .workType(request.getWorkType())
                .scheduledStart(request.getScheduledStart())
                .scheduledEnd(request.getScheduledEnd())
                .notes(trimToNull(request.getNotes()))
                .status(WorkOrderStatus.NEW)
                .createdBy(createdBy)
                .build();

        WorkOrder saved = workOrderRepository.saveAndFlush(workOrder);
        saved.setWorkOrderNumber(formatWorkOrderNumber(saved.getId()));
        slaService.applySnapshot(saved, customer);
        workOrderRepository.save(saved);

        return WorkOrderResponse.fromEntity(findWorkOrder(saved.getId()));
    }

    @Override
    @Transactional
    public WorkOrderResponse updateWorkOrder(Long id, UpdateWorkOrderRequest request, String currentUsername) {
        WorkOrder workOrder = findAccessibleWorkOrder(id, currentUsername);
        assertEditable(workOrder);
        validateSchedule(request.getScheduledStart(), request.getScheduledEnd());

        Customer customer = findCustomer(request.getCustomerId());
        Site site = findSite(request.getSiteId());
        assertSiteBelongsToCustomer(site, customer.getId());

        workOrder.setTitle(trimToNull(request.getTitle()));
        workOrder.setDescription(trimToNull(request.getDescription()));
        workOrder.setCustomer(customer);
        workOrder.setSite(site);
        workOrder.setPriority(request.getPriority());
        workOrder.setWorkType(request.getWorkType());
        workOrder.setScheduledStart(request.getScheduledStart());
        workOrder.setScheduledEnd(request.getScheduledEnd());
        workOrder.setNotes(trimToNull(request.getNotes()));

        workOrderRepository.save(workOrder);
        return WorkOrderResponse.fromEntity(findWorkOrder(id));
    }

    @Override
    @Transactional
    public WorkOrderResponse assignWorkOrder(Long id, AssignWorkOrderRequest request, String currentUsername) {
        User currentUser = findCurrentUser(currentUsername);
        if (isTechnician(currentUser)) {
            throw new ApiException("Technicians are not permitted to assign work orders.", HttpStatus.FORBIDDEN);
        }
        WorkOrder workOrder = findAccessibleWorkOrder(id, currentUsername);
        User technician = findAssignableTechnician(request.getTechnicianId());
        Long previousTechnicianId = workOrder.getAssignedTechnician() != null
                ? workOrder.getAssignedTechnician().getId()
                : null;
        boolean technicianChanged = !technician.getId().equals(previousTechnicianId);

        if (workOrder.getStatus() == WorkOrderStatus.NEW) {
            transitionTo(workOrder, WorkOrderStatus.ASSIGNED);
        } else if (workOrder.getStatus() != WorkOrderStatus.ASSIGNED) {
            throw new ApiException(
                    "Work order can only be assigned when status is NEW or ASSIGNED.",
                    HttpStatus.CONFLICT
            );
        }

        workOrder.setAssignedTechnician(technician);
        slaService.recordResponseIfNeeded(workOrder, LocalDateTime.now());
        workOrderRepository.save(workOrder);
        WorkOrder saved = findWorkOrder(id);
        if (technicianChanged) {
            notifySafely(() -> notificationService.notifyWorkOrderAssigned(technician, saved));
        }
        notifySafely(() -> notificationService.notifySlaIfNeeded(saved));
        return WorkOrderResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public WorkOrderResponse startWorkOrder(Long id, String currentUsername) {
        WorkOrder workOrder = findAccessibleWorkOrder(id, currentUsername);
        if (workOrder.getAssignedTechnician() == null) {
            throw new ApiException("Work order must be assigned before it can be started.", HttpStatus.CONFLICT);
        }
        transitionTo(workOrder, WorkOrderStatus.IN_PROGRESS);
        if (workOrder.getActualStart() == null) {
            workOrder.setActualStart(LocalDateTime.now());
        }
        workOrderRepository.save(workOrder);
        return respondAfterStatusChange(id, WorkOrderStatus.IN_PROGRESS);
    }

    @Override
    @Transactional
    public WorkOrderResponse holdWorkOrder(Long id, String currentUsername) {
        WorkOrder workOrder = findAccessibleWorkOrder(id, currentUsername);
        transitionTo(workOrder, WorkOrderStatus.ON_HOLD);
        workOrderRepository.save(workOrder);
        return respondAfterStatusChange(id, WorkOrderStatus.ON_HOLD);
    }

    @Override
    @Transactional
    public WorkOrderResponse resumeWorkOrder(Long id, String currentUsername) {
        WorkOrder workOrder = findAccessibleWorkOrder(id, currentUsername);
        transitionTo(workOrder, WorkOrderStatus.IN_PROGRESS);
        workOrderRepository.save(workOrder);
        return respondAfterStatusChange(id, WorkOrderStatus.IN_PROGRESS);
    }

    @Override
    @Transactional
    public WorkOrderResponse completeWorkOrder(Long id, String currentUsername) {
        WorkOrder workOrder = findAccessibleWorkOrder(id, currentUsername);
        transitionTo(workOrder, WorkOrderStatus.COMPLETED);
        if (workOrder.getActualEnd() == null) {
            workOrder.setActualEnd(LocalDateTime.now());
        }
        slaService.recordResolutionIfNeeded(workOrder, LocalDateTime.now());
        workOrderRepository.save(workOrder);
        WorkOrder saved = findWorkOrder(id);
        notifySafely(() -> notificationService.notifyWorkOrderStatusChanged(saved, WorkOrderStatus.COMPLETED));
        notifySafely(() -> notificationService.notifySlaIfNeeded(saved));
        return WorkOrderResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public WorkOrderResponse closeWorkOrder(Long id, String currentUsername) {
        WorkOrder workOrder = findAccessibleWorkOrder(id, currentUsername);
        transitionTo(workOrder, WorkOrderStatus.CLOSED);
        workOrderRepository.save(workOrder);
        return respondAfterStatusChange(id, WorkOrderStatus.CLOSED);
    }

    @Override
    @Transactional
    public WorkOrderResponse cancelWorkOrder(Long id, String currentUsername) {
        WorkOrder workOrder = findAccessibleWorkOrder(id, currentUsername);
        transitionTo(workOrder, WorkOrderStatus.CANCELLED);
        workOrderRepository.save(workOrder);
        return respondAfterStatusChange(id, WorkOrderStatus.CANCELLED);
    }

    @Override
    @Transactional
    public void deleteWorkOrder(Long id, String currentUsername) {
        WorkOrder workOrder = findAccessibleWorkOrder(id, currentUsername);
        if (NON_DELETABLE_STATUSES.contains(workOrder.getStatus())) {
            throw new ApiException(
                    "Completed or closed work orders cannot be deleted.",
                    HttpStatus.CONFLICT
            );
        }
        if (workOrderPartRepository.existsByWorkOrderId(id)) {
            throw new ApiException(
                    "Cannot delete work order because parts have been used on it.",
                    HttpStatus.CONFLICT
            );
        }
        if (timeLogRepository.existsByWorkOrderId(id)) {
            throw new ApiException(
                    "Cannot delete work order because time logs exist for it.",
                    HttpStatus.CONFLICT
            );
        }
        workOrderRepository.delete(workOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderSlaResponse getWorkOrderSla(Long id, String currentUsername) {
        return slaService.getWorkOrderSla(id, currentUsername);
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
        return slaService.getWorkOrderSlaList(
                page, size, sort, search, status, priority, customerId, siteId, technicianId, slaStatus, currentUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAssignableTechnicians() {
        return userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.TECHNICIAN, true)
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    void assertValidTransition(WorkOrderStatus current, WorkOrderStatus next) {
        if (current == null || next == null) {
            throw new ApiException("Work order status is required.", HttpStatus.BAD_REQUEST);
        }
        Set<WorkOrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new ApiException(
                    "Invalid work order status transition from " + current + " to " + next + ".",
                    HttpStatus.CONFLICT
            );
        }
    }

    private void transitionTo(WorkOrder workOrder, WorkOrderStatus nextStatus) {
        assertValidTransition(workOrder.getStatus(), nextStatus);
        workOrder.setStatus(nextStatus);
    }

    private WorkOrderResponse respondAfterStatusChange(Long id, WorkOrderStatus status) {
        WorkOrder saved = findWorkOrder(id);
        notifySafely(() -> notificationService.notifyWorkOrderStatusChanged(saved, status));
        return WorkOrderResponse.fromEntity(saved);
    }

    private void notifySafely(Runnable action) {
        try {
            action.run();
        } catch (Exception ignored) {
            // Notification failures must not roll back work-order changes.
        }
    }

    private void assertEditable(WorkOrder workOrder) {
        if (TERMINAL_UPDATE_STATUSES.contains(workOrder.getStatus())) {
            throw new ApiException(
                    "Work orders that are completed, closed, or cancelled cannot be updated.",
                    HttpStatus.CONFLICT
            );
        }
    }

    private void assertSiteBelongsToCustomer(Site site, Long customerId) {
        if (site.getCustomer() == null || !customerId.equals(site.getCustomer().getId())) {
            throw new ApiException(
                    "The selected site does not belong to the selected customer.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateSchedule(LocalDateTime scheduledStart, LocalDateTime scheduledEnd) {
        if (scheduledStart != null && scheduledEnd != null && scheduledEnd.isBefore(scheduledStart)) {
            throw new ApiException("Scheduled end must not be before scheduled start.", HttpStatus.BAD_REQUEST);
        }
    }

    private User findAssignableTechnician(Long technicianId) {
        User user = userRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + technicianId));

        if (user.getRole() != Role.TECHNICIAN) {
            throw new ApiException(
                    "Only users with the TECHNICIAN role can be assigned to a work order.",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (!user.isEnabled()) {
            throw new ApiException("The selected technician is inactive and cannot be assigned.", HttpStatus.BAD_REQUEST);
        }
        return user;
    }

    private WorkOrder findAccessibleWorkOrder(Long id, String currentUsername) {
        WorkOrder workOrder = findWorkOrder(id);
        assertTechnicianOwnsWorkOrder(workOrder, currentUsername, "Work order not found with id: " + id);
        return workOrder;
    }

    private void assertTechnicianOwnsWorkOrder(WorkOrder workOrder, String currentUsername, String notFoundMessage) {
        User currentUser = findCurrentUser(currentUsername);
        if (!isTechnician(currentUser)) {
            return;
        }

        User assignedTechnician = workOrder.getAssignedTechnician();
        if (assignedTechnician == null || !currentUser.getId().equals(assignedTechnician.getId())) {
            throw new ResourceNotFoundException(notFoundMessage);
        }
    }

    private Long resolveScopedTechnicianId(User currentUser, Long requestedTechnicianId) {
        if (isTechnician(currentUser)) {
            return currentUser.getId();
        }
        return requestedTechnicianId;
    }

    private boolean isTechnician(User user) {
        return user != null && user.getRole() == Role.TECHNICIAN;
    }

    private WorkOrder findWorkOrder(Long id) {
        return workOrderRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with id: " + id));
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findByIdWithSlaPolicy(customerId)
                .or(() -> customerRepository.findById(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
    }

    private Site findSite(Long siteId) {
        return siteRepository.findByIdWithCustomer(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found with id: " + siteId));
    }

    private User findCurrentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ApiException("Authenticated user could not be resolved.", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByUserEmail(username.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }

    private String formatWorkOrderNumber(Long id) {
        return String.format("WO-%06d", id);
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

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
