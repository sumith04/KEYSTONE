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
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.ServiceRequestRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    private static final String CUSTOMER_EMAIL = "acme@keystone.com";
    private static final String DISPATCHER_EMAIL = "dispatcher@keystone.com";

    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private SiteRepository siteRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private CustomerAccessGuard customerAccessGuard;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ServiceRequestServiceImpl serviceRequestService;

    private Customer customerA;
    private Customer customerB;
    private Site siteA;
    private Site siteB;
    private ServiceRequest requestA;

    @BeforeEach
    void setUp() {
        customerA = Customer.builder().id(1L).customerCode("ACME001").companyName("Acme").email(CUSTOMER_EMAIL).build();
        customerB = Customer.builder().id(2L).customerCode("NORTH01").companyName("North").email("north@keystone.com").build();
        siteA = Site.builder().id(10L).siteCode("HQ-01").siteName("HQ").customer(customerA).build();
        siteB = Site.builder().id(11L).siteCode("NC-01").siteName("North").customer(customerB).build();
        requestA = ServiceRequest.builder()
                .id(50L)
                .requestNumber("SR-000050")
                .customer(customerA)
                .site(siteA)
                .title("HVAC repair")
                .description("Lobby unit failed")
                .priority(WorkOrderPriority.HIGH)
                .status(ServiceRequestStatus.SUBMITTED)
                .build();
    }

    @Test
    void createMyRequest_ShouldScopeToAuthenticatedCustomer() {
        when(customerAccessGuard.requireLinkedCustomer(CUSTOMER_EMAIL)).thenReturn(customerA);
        when(siteRepository.findByIdWithCustomer(10L)).thenReturn(Optional.of(siteA));
        when(serviceRequestRepository.saveAndFlush(any(ServiceRequest.class))).thenAnswer(invocation -> {
            ServiceRequest saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceRequestRepository.findByIdWithRelations(50L)).thenReturn(Optional.of(requestA));

        ServiceRequestResponse response = serviceRequestService.createMyRequest(
                CreateServiceRequest.builder()
                        .siteId(10L)
                        .title("HVAC repair")
                        .description("Lobby unit failed")
                        .priority(WorkOrderPriority.HIGH)
                        .build(),
                CUSTOMER_EMAIL
        );

        assertEquals("SR-000050", response.getRequestNumber());
        assertEquals(1L, response.getCustomerId());
        verify(notificationService).notifyServiceRequestSubmitted(any(ServiceRequest.class));
        ArgumentCaptor<ServiceRequest> captor = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(serviceRequestRepository).saveAndFlush(captor.capture());
        assertEquals(customerA, captor.getValue().getCustomer());
        assertEquals(ServiceRequestStatus.SUBMITTED, captor.getValue().getStatus());
    }

    @Test
    void createMyRequest_WhenSiteBelongsToAnotherCustomer_ShouldReject() {
        when(customerAccessGuard.requireLinkedCustomer(CUSTOMER_EMAIL)).thenReturn(customerA);
        when(siteRepository.findByIdWithCustomer(11L)).thenReturn(Optional.of(siteB));

        ApiException exception = assertThrows(ApiException.class, () -> serviceRequestService.createMyRequest(
                CreateServiceRequest.builder()
                        .siteId(11L)
                        .title("HVAC repair")
                        .description("Lobby unit failed")
                        .priority(WorkOrderPriority.HIGH)
                        .build(),
                CUSTOMER_EMAIL
        ));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(serviceRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void getMyRequest_WhenOwned_ShouldReturn() {
        when(customerAccessGuard.requireLinkedCustomer(CUSTOMER_EMAIL)).thenReturn(customerA);
        when(serviceRequestRepository.findByIdAndCustomerIdWithRelations(50L, 1L)).thenReturn(Optional.of(requestA));

        ServiceRequestResponse response = serviceRequestService.getMyRequest(50L, CUSTOMER_EMAIL);
        assertEquals(50L, response.getId());
    }

    @Test
    void getMyRequest_WhenOtherCustomer_ShouldReturnNotFound() {
        when(customerAccessGuard.requireLinkedCustomer(CUSTOMER_EMAIL)).thenReturn(customerA);
        when(serviceRequestRepository.findByIdAndCustomerIdWithRelations(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceRequestService.getMyRequest(99L, CUSTOMER_EMAIL));
    }

    @Test
    void getMyRequests_ShouldQueryByCustomerId() {
        when(customerAccessGuard.requireLinkedCustomer(CUSTOMER_EMAIL)).thenReturn(customerA);
        when(serviceRequestRepository.searchForCustomer(eq(1L), isNull(), eq(ServiceRequestStatus.SUBMITTED), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(requestA)));

        ServiceRequestPageResponse page = serviceRequestService.getMyRequests(
                0, 10, "createdAt", null, ServiceRequestStatus.SUBMITTED, null, CUSTOMER_EMAIL);

        assertEquals(1, page.getContent().size());
        verify(serviceRequestRepository).searchForCustomer(eq(1L), isNull(), eq(ServiceRequestStatus.SUBMITTED), isNull(), any(Pageable.class));
    }

    @Test
    void updateMyRequest_WhenNotSubmitted_ShouldConflict() {
        requestA.setStatus(ServiceRequestStatus.ACKNOWLEDGED);
        when(customerAccessGuard.requireLinkedCustomer(CUSTOMER_EMAIL)).thenReturn(customerA);
        when(serviceRequestRepository.findByIdAndCustomerIdWithRelations(50L, 1L)).thenReturn(Optional.of(requestA));

        ApiException exception = assertThrows(ApiException.class, () -> serviceRequestService.updateMyRequest(
                50L,
                UpdateServiceRequest.builder()
                        .siteId(10L)
                        .title("Updated")
                        .description("Updated desc")
                        .priority(WorkOrderPriority.MEDIUM)
                        .build(),
                CUSTOMER_EMAIL
        ));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void cancelMyRequest_WhenSubmitted_ShouldCancel() {
        when(customerAccessGuard.requireLinkedCustomer(CUSTOMER_EMAIL)).thenReturn(customerA);
        when(serviceRequestRepository.findByIdAndCustomerIdWithRelations(50L, 1L)).thenReturn(Optional.of(requestA));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.cancelMyRequest(50L, CUSTOMER_EMAIL);
        assertEquals(ServiceRequestStatus.CANCELLED, requestA.getStatus());
        assertEquals(ServiceRequestStatus.CANCELLED, response.getStatus());
    }

    @Test
    void convertToWorkOrder_ShouldCreateWorkOrderAndMarkConverted() {
        requestA.setStatus(ServiceRequestStatus.IN_REVIEW);
        WorkOrder workOrder = WorkOrder.builder()
                .id(7L)
                .workOrderNumber("WO-000007")
                .customer(customerA)
                .site(siteA)
                .title(requestA.getTitle())
                .description(requestA.getDescription())
                .priority(WorkOrderPriority.HIGH)
                .status(WorkOrderStatus.NEW)
                .build();
        when(serviceRequestRepository.findByIdWithRelations(50L)).thenReturn(Optional.of(requestA));
        when(workOrderService.createWorkOrder(any(CreateWorkOrderRequest.class), eq(DISPATCHER_EMAIL)))
                .thenReturn(WorkOrderResponse.builder().id(7L).workOrderNumber("WO-000007").build());
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.convertToWorkOrder(50L, DISPATCHER_EMAIL);

        assertEquals(ServiceRequestStatus.CONVERTED_TO_WORK_ORDER, requestA.getStatus());
        assertEquals(7L, requestA.getWorkOrder().getId());
        assertEquals("WO-000007", response.getWorkOrderNumber());
        ArgumentCaptor<CreateWorkOrderRequest> captor = ArgumentCaptor.forClass(CreateWorkOrderRequest.class);
        verify(workOrderService).createWorkOrder(captor.capture(), eq(DISPATCHER_EMAIL));
        assertEquals(1L, captor.getValue().getCustomerId());
        assertEquals(10L, captor.getValue().getSiteId());
        assertEquals("HVAC repair", captor.getValue().getTitle());
        assertEquals(WorkOrderPriority.HIGH, captor.getValue().getPriority());
        verify(notificationService).notifyServiceRequestUpdated(any(ServiceRequest.class));
    }

    @Test
    void convertToWorkOrder_WhenAlreadyConverted_ShouldConflict() {
        requestA.setStatus(ServiceRequestStatus.CONVERTED_TO_WORK_ORDER);
        requestA.setWorkOrder(WorkOrder.builder().id(7L).build());
        when(serviceRequestRepository.findByIdWithRelations(50L)).thenReturn(Optional.of(requestA));

        ApiException exception = assertThrows(
                ApiException.class, () -> serviceRequestService.convertToWorkOrder(50L, DISPATCHER_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(workOrderService, never()).createWorkOrder(any(), any());
    }

    @Test
    void convertToWorkOrder_WhenSubmitted_ShouldConflict() {
        when(serviceRequestRepository.findByIdWithRelations(50L)).thenReturn(Optional.of(requestA));

        ApiException exception = assertThrows(
                ApiException.class, () -> serviceRequestService.convertToWorkOrder(50L, DISPATCHER_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void acknowledge_WhenInvalidState_ShouldConflict() {
        requestA.setStatus(ServiceRequestStatus.IN_REVIEW);
        when(serviceRequestRepository.findByIdWithRelations(50L)).thenReturn(Optional.of(requestA));

        ApiException exception = assertThrows(
                ApiException.class, () -> serviceRequestService.acknowledge(50L, DISPATCHER_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }
}
