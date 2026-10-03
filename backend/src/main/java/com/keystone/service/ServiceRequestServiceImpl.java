package com.keystone.service;

import com.keystone.dto.CreateServiceRequest;
import com.keystone.dto.CreateWorkOrderRequest;
import com.keystone.dto.ServiceRequestPageResponse;
import com.keystone.dto.ServiceRequestResponse;
import com.keystone.dto.UpdateServiceRequest;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.entity.Customer;
import com.keystone.entity.ServiceRequest;
import com.keystone.entity.Site;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.ServiceRequestStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkType;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.ServiceRequestRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "createdAt", "updatedAt", "requestNumber", "title", "status", "priority", "requestedAt"
    );

    private final ServiceRequestRepository serviceRequestRepository;
    private final SiteRepository siteRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderService workOrderService;
    private final CustomerAccessGuard customerAccessGuard;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestPageResponse getMyRequests(
            int page,
            int size,
            String sort,
            String search,
            ServiceRequestStatus status,
            WorkOrderPriority priority,
            String currentUsername) {

        Customer customer = customerAccessGuard.requireLinkedCustomer(currentUsername);
        Page<ServiceRequest> result = serviceRequestRepository.searchForCustomer(
                customer.getId(), normalizeSearch(search), status, priority, buildPageable(page, size, sort));
        return toPage(result);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestResponse getMyRequest(Long id, String currentUsername) {
        Customer customer = customerAccessGuard.requireLinkedCustomer(currentUsername);
        return ServiceRequestResponse.fromEntity(findOwned(id, customer.getId()));
    }

    @Override
    @Transactional
    public ServiceRequestResponse createMyRequest(CreateServiceRequest request, String currentUsername) {
        Customer customer = customerAccessGuard.requireLinkedCustomer(currentUsername);
        Site site = requireOwnedSite(request.getSiteId(), customer.getId());

        ServiceRequest saved = serviceRequestRepository.saveAndFlush(ServiceRequest.builder()
                .customer(customer)
                .site(site)
                .title(trimToNull(request.getTitle()))
                .description(trimToNull(request.getDescription()))
                .priority(request.getPriority())
                .status(ServiceRequestStatus.SUBMITTED)
                .preferredDate(request.getPreferredDate())
                .contactName(trimToNull(request.getContactName()))
                .contactPhone(trimToNull(request.getContactPhone()))
                .build());
        saved.setRequestNumber(formatRequestNumber(saved.getId()));
        serviceRequestRepository.save(saved);

        ServiceRequest created = findById(saved.getId());
        notificationService.notifyServiceRequestSubmitted(created);
        return ServiceRequestResponse.fromEntity(created);
    }

    @Override
    @Transactional
    public ServiceRequestResponse updateMyRequest(Long id, UpdateServiceRequest request, String currentUsername) {
        Customer customer = customerAccessGuard.requireLinkedCustomer(currentUsername);
        ServiceRequest existing = findOwned(id, customer.getId());
        assertCustomerEditable(existing);
        Site site = requireOwnedSite(request.getSiteId(), customer.getId());

        existing.setSite(site);
        existing.setTitle(trimToNull(request.getTitle()));
        existing.setDescription(trimToNull(request.getDescription()));
        existing.setPriority(request.getPriority());
        existing.setPreferredDate(request.getPreferredDate());
        existing.setContactName(trimToNull(request.getContactName()));
        existing.setContactPhone(trimToNull(request.getContactPhone()));
        serviceRequestRepository.save(existing);
        return ServiceRequestResponse.fromEntity(findOwned(id, customer.getId()));
    }

    @Override
    @Transactional
    public ServiceRequestResponse cancelMyRequest(Long id, String currentUsername) {
        Customer customer = customerAccessGuard.requireLinkedCustomer(currentUsername);
        ServiceRequest existing = findOwned(id, customer.getId());
        if (existing.getStatus() != ServiceRequestStatus.SUBMITTED) {
            throw new ApiException("Only submitted requests can be cancelled.", HttpStatus.CONFLICT);
        }
        existing.setStatus(ServiceRequestStatus.CANCELLED);
        serviceRequestRepository.save(existing);
        return ServiceRequestResponse.fromEntity(findOwned(id, customer.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestPageResponse getRequests(
            int page,
            int size,
            String sort,
            String search,
            ServiceRequestStatus status,
            WorkOrderPriority priority,
            Long customerId) {

        Page<ServiceRequest> result = serviceRequestRepository.searchAll(
                normalizeSearch(search), status, priority, customerId, buildPageable(page, size, sort));
        return toPage(result);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestResponse getRequest(Long id) {
        return ServiceRequestResponse.fromEntity(findById(id));
    }

    @Override
    @Transactional
    public ServiceRequestResponse acknowledge(Long id, String currentUsername) {
        return transition(id, ServiceRequestStatus.SUBMITTED, ServiceRequestStatus.ACKNOWLEDGED, currentUsername);
    }

    @Override
    @Transactional
    public ServiceRequestResponse markInReview(Long id, String currentUsername) {
        return transition(id, ServiceRequestStatus.ACKNOWLEDGED, ServiceRequestStatus.IN_REVIEW, currentUsername);
    }

    @Override
    @Transactional
    public ServiceRequestResponse reject(Long id, String currentUsername) {
        ServiceRequest request = findById(id);
        if (request.getStatus() != ServiceRequestStatus.IN_REVIEW
                && request.getStatus() != ServiceRequestStatus.ACKNOWLEDGED) {
            throw new ApiException(
                    "Invalid service request status transition from " + request.getStatus() + " to REJECTED.",
                    HttpStatus.CONFLICT
            );
        }
        request.setStatus(ServiceRequestStatus.REJECTED);
        serviceRequestRepository.save(request);
        ServiceRequest updated = findById(id);
        notificationService.notifyServiceRequestUpdated(updated);
        return ServiceRequestResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public ServiceRequestResponse convertToWorkOrder(Long id, String currentUsername) {
        ServiceRequest request = findById(id);
        if (request.getStatus() == ServiceRequestStatus.CONVERTED_TO_WORK_ORDER || request.getWorkOrder() != null) {
            throw new ApiException("This service request has already been converted to a work order.", HttpStatus.CONFLICT);
        }
        if (request.getStatus() != ServiceRequestStatus.ACKNOWLEDGED
                && request.getStatus() != ServiceRequestStatus.IN_REVIEW) {
            throw new ApiException(
                    "Only acknowledged or in-review service requests can be converted.",
                    HttpStatus.CONFLICT
            );
        }

        CreateWorkOrderRequest workOrderRequest = CreateWorkOrderRequest.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .customerId(request.getCustomer().getId())
                .siteId(request.getSite().getId())
                .priority(request.getPriority())
                .workType(WorkType.CORRECTIVE_MAINTENANCE)
                .scheduledStart(request.getPreferredDate())
                .notes("Created from service request " + request.getRequestNumber() + ".")
                .build();

        WorkOrderResponse created = workOrderService.createWorkOrder(workOrderRequest, currentUsername);
        WorkOrder workOrder = workOrderRepository.findByIdWithRelations(created.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with id: " + created.getId()));

        request.setWorkOrder(workOrder);
        request.setStatus(ServiceRequestStatus.CONVERTED_TO_WORK_ORDER);
        serviceRequestRepository.save(request);

        ServiceRequest updated = findById(id);
        notificationService.notifyServiceRequestUpdated(updated);
        return ServiceRequestResponse.fromEntity(updated);
    }

    private ServiceRequestResponse transition(
            Long id,
            ServiceRequestStatus expected,
            ServiceRequestStatus next,
            String currentUsername) {
        ServiceRequest request = findById(id);
        if (request.getStatus() != expected) {
            throw new ApiException(
                    "Invalid service request status transition from " + request.getStatus() + " to " + next + ".",
                    HttpStatus.CONFLICT
            );
        }
        request.setStatus(next);
        serviceRequestRepository.save(request);
        ServiceRequest updated = findById(id);
        notificationService.notifyServiceRequestUpdated(updated);
        return ServiceRequestResponse.fromEntity(updated);
    }

    private ServiceRequest findOwned(Long id, Long customerId) {
        return serviceRequestRepository.findByIdAndCustomerIdWithRelations(id, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + id));
    }

    private ServiceRequest findById(Long id) {
        return serviceRequestRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + id));
    }

    private Site requireOwnedSite(Long siteId, Long customerId) {
        Site site = siteRepository.findByIdWithCustomer(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found with id: " + siteId));
        if (site.getCustomer() == null || !customerId.equals(site.getCustomer().getId())) {
            throw new ApiException("The selected site does not belong to your customer account.", HttpStatus.BAD_REQUEST);
        }
        return site;
    }

    private void assertCustomerEditable(ServiceRequest request) {
        if (request.getStatus() != ServiceRequestStatus.SUBMITTED) {
            throw new ApiException("Only submitted requests can be updated.", HttpStatus.CONFLICT);
        }
    }

    private ServiceRequestPageResponse toPage(Page<ServiceRequest> page) {
        return ServiceRequestPageResponse.builder()
                .content(page.getContent().stream().map(ServiceRequestResponse::fromEntity).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
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

    private String formatRequestNumber(Long id) {
        return String.format("SR-%06d", id);
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
