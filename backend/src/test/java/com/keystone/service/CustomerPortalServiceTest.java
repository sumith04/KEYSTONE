package com.keystone.service;

import com.keystone.dto.CustomerProfileResponse;
import com.keystone.dto.CustomerWorkOrderResponse;
import com.keystone.entity.Customer;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.CustomerStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.enums.WorkType;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.ServiceRequestRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerPortalServiceTest {

    private static final String CUSTOMER_EMAIL = "acme@keystone.com";

    @Mock
    private CustomerAccessGuard customerAccessGuard;
    @Mock
    private SiteRepository siteRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private CustomerPortalServiceImpl customerPortalService;

    private Customer customerA;
    private WorkOrder workOrderA;

    @BeforeEach
    void setUp() {
        customerA = Customer.builder()
                .id(1L)
                .customerCode("ACME001")
                .companyName("Acme Facilities")
                .email(CUSTOMER_EMAIL)
                .status(CustomerStatus.ACTIVE)
                .build();
        workOrderA = WorkOrder.builder()
                .id(7L)
                .workOrderNumber("WO-000007")
                .title("HVAC repair")
                .description("Lobby unit")
                .customer(customerA)
                .status(WorkOrderStatus.IN_PROGRESS)
                .priority(WorkOrderPriority.HIGH)
                .workType(WorkType.CORRECTIVE_MAINTENANCE)
                .build();
    }

    @Test
    void getProfile_ShouldReturnLinkedCustomerOnly() {
        when(customerAccessGuard.requireLinkedCustomer(CUSTOMER_EMAIL)).thenReturn(customerA);

        CustomerProfileResponse profile = customerPortalService.getProfile(CUSTOMER_EMAIL);

        assertEquals("Acme Facilities", profile.getCompanyName());
        assertEquals("ACME001", profile.getCustomerCode());
    }

    @Test
    void getWorkOrder_WhenOwned_ShouldHideInternalNotes() {
        when(customerAccessGuard.requireLinkedCustomer(CUSTOMER_EMAIL)).thenReturn(customerA);
        workOrderA.setNotes("internal routing notes");
        when(workOrderRepository.findByIdAndCustomerIdWithRelations(7L, 1L)).thenReturn(Optional.of(workOrderA));
        when(serviceRequestRepository.findByWorkOrderId(7L)).thenReturn(Optional.empty());

        CustomerWorkOrderResponse response = customerPortalService.getWorkOrder(7L, CUSTOMER_EMAIL);

        assertEquals("WO-000007", response.getWorkOrderNumber());
        assertEquals("HVAC repair", response.getTitle());
        assertNull(response.getServiceRequestId());
    }

    @Test
    void getWorkOrder_WhenOtherCustomer_ShouldReturnNotFound() {
        when(customerAccessGuard.requireLinkedCustomer(CUSTOMER_EMAIL)).thenReturn(customerA);
        when(workOrderRepository.findByIdAndCustomerIdWithRelations(8L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerPortalService.getWorkOrder(8L, CUSTOMER_EMAIL));
    }

    @Test
    void getWorkOrders_ShouldForceCustomerScope() {
        when(customerAccessGuard.requireLinkedCustomer(CUSTOMER_EMAIL)).thenReturn(customerA);
        when(workOrderRepository.searchWorkOrders(isNull(), isNull(), isNull(), eq(1L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(workOrderA)));
        when(serviceRequestRepository.findByWorkOrderId(7L)).thenReturn(Optional.empty());

        assertEquals(1, customerPortalService.getWorkOrders(0, 10, "createdAt", null, null, CUSTOMER_EMAIL)
                .getContent().size());
    }
}
