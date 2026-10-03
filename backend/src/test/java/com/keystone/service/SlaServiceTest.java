package com.keystone.service;

import com.keystone.dto.CreateSlaPolicyRequest;
import com.keystone.dto.SlaPolicyPageResponse;
import com.keystone.dto.SlaPolicyResponse;
import com.keystone.dto.UpdateSlaPolicyRequest;
import com.keystone.dto.WorkOrderSlaResponse;
import com.keystone.entity.Customer;
import com.keystone.entity.SlaPolicy;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.Role;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SlaPolicyRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaServiceTest {

    @Mock
    private SlaPolicyRepository slaPolicyRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderAccessGuard workOrderAccessGuard;

    @InjectMocks
    private SlaServiceImpl slaService;

    private SlaPolicy gold;
    private Customer customer;
    private WorkOrder workOrder;
    private User technician;

    @BeforeEach
    void setUp() {
        gold = SlaPolicy.builder()
                .id(3L)
                .name("Gold")
                .description("Priority response")
                .priority(WorkOrderPriority.HIGH)
                .responseTimeMinutes(60)
                .resolutionTimeMinutes(240)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customer = Customer.builder()
                .id(1L)
                .customerCode("ACME001")
                .companyName("Acme Facilities")
                .slaPolicy(gold)
                .build();

        technician = User.builder()
                .id(25L)
                .userEmail("tech.a@keystone.com")
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();

        workOrder = WorkOrder.builder()
                .id(7L)
                .workOrderNumber("WO-000007")
                .title("Replace HVAC filter")
                .customer(customer)
                .assignedTechnician(technician)
                .status(WorkOrderStatus.NEW)
                .createdAt(LocalDateTime.of(2026, 10, 3, 8, 0))
                .build();
    }

    @Test
    void createPolicy_WhenValid_ShouldSave() {
        CreateSlaPolicyRequest request = CreateSlaPolicyRequest.builder()
                .name(" Gold ")
                .priority(WorkOrderPriority.HIGH)
                .responseTimeMinutes(60)
                .resolutionTimeMinutes(240)
                .active(true)
                .build();
        when(slaPolicyRepository.existsByNameIgnoreCase("Gold")).thenReturn(false);
        when(slaPolicyRepository.save(any(SlaPolicy.class))).thenReturn(gold);

        SlaPolicyResponse response = slaService.createPolicy(request);

        ArgumentCaptor<SlaPolicy> captor = ArgumentCaptor.forClass(SlaPolicy.class);
        verify(slaPolicyRepository).save(captor.capture());
        assertEquals("Gold", captor.getValue().getName());
        assertEquals(60, captor.getValue().getResponseTimeMinutes());
        assertEquals("Gold", response.getName());
    }

    @Test
    void createPolicy_WhenResolutionShorterThanResponse_ShouldReject() {
        CreateSlaPolicyRequest request = CreateSlaPolicyRequest.builder()
                .name("Bad")
                .priority(WorkOrderPriority.LOW)
                .responseTimeMinutes(120)
                .resolutionTimeMinutes(30)
                .build();

        ApiException exception = assertThrows(ApiException.class, () -> slaService.createPolicy(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void createPolicy_WhenDuplicateName_ShouldConflict() {
        when(slaPolicyRepository.existsByNameIgnoreCase("Gold")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> slaService.createPolicy(CreateSlaPolicyRequest.builder()
                .name("Gold")
                .priority(WorkOrderPriority.HIGH)
                .responseTimeMinutes(60)
                .resolutionTimeMinutes(120)
                .build()));
    }

    @Test
    void getPolicies_ShouldUseRepositoryFilters() {
        when(slaPolicyRepository.searchPolicies(eq("gold"), eq(WorkOrderPriority.HIGH), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(gold)));

        SlaPolicyPageResponse response = slaService.getPolicies(0, 10, "name,asc", "gold", WorkOrderPriority.HIGH, true);

        assertEquals(1, response.getContent().size());
        assertEquals("Gold", response.getContent().get(0).getName());
    }

    @Test
    void updatePolicy_WhenMissing_Should404() {
        when(slaPolicyRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> slaService.updatePolicy(99L, UpdateSlaPolicyRequest.builder()
                .name("Gold")
                .priority(WorkOrderPriority.HIGH)
                .responseTimeMinutes(60)
                .resolutionTimeMinutes(120)
                .active(true)
                .build()));
    }

    @Test
    void deletePolicy_WhenAssignedToCustomer_ShouldConflict() {
        when(slaPolicyRepository.findById(3L)).thenReturn(Optional.of(gold));
        when(customerRepository.existsBySlaPolicyId(3L)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> slaService.deletePolicy(3L));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(slaPolicyRepository, never()).delete(any());
    }

    @Test
    void deletePolicy_WhenUnused_ShouldDelete() {
        when(slaPolicyRepository.findById(3L)).thenReturn(Optional.of(gold));
        when(customerRepository.existsBySlaPolicyId(3L)).thenReturn(false);
        when(workOrderRepository.existsBySlaPolicyId(3L)).thenReturn(false);

        slaService.deletePolicy(3L);

        verify(slaPolicyRepository).delete(gold);
    }

    @Test
    void applySnapshot_WhenCustomerHasActivePolicy_ShouldSetDeadlines() {
        slaService.applySnapshot(workOrder, customer);

        assertEquals(gold, workOrder.getSlaPolicy());
        assertEquals("Gold", workOrder.getSlaPolicyName());
        assertEquals(LocalDateTime.of(2026, 10, 3, 9, 0), workOrder.getSlaResponseDueAt());
        assertEquals(LocalDateTime.of(2026, 10, 3, 12, 0), workOrder.getSlaResolutionDueAt());
    }

    @Test
    void applySnapshot_WhenNoPolicy_ShouldLeaveNull() {
        customer.setSlaPolicy(null);
        slaService.applySnapshot(workOrder, customer);
        assertNull(workOrder.getSlaResponseDueAt());
        assertNull(workOrder.getSlaResolutionDueAt());
    }

    @Test
    void applySnapshot_WhenPolicyInactive_ShouldLeaveNull() {
        gold.setActive(false);
        slaService.applySnapshot(workOrder, customer);
        assertNull(workOrder.getSlaResponseDueAt());
        assertNull(workOrder.getSlaPolicyName());
    }

    @Test
    void applySnapshot_WhenPolicyChangesLater_ShouldNotRewriteExistingDeadlines() {
        slaService.applySnapshot(workOrder, customer);
        LocalDateTime originalResponseDue = workOrder.getSlaResponseDueAt();
        gold.setResponseTimeMinutes(15);
        gold.setName("Gold Plus");

        assertEquals(originalResponseDue, workOrder.getSlaResponseDueAt());
        assertEquals("Gold", workOrder.getSlaPolicyName());
    }

    @Test
    void recordResponse_ShouldSetOnceAndCalculateBreach() {
        workOrder.setSlaResponseDueAt(LocalDateTime.of(2026, 10, 3, 9, 0));
        LocalDateTime first = LocalDateTime.of(2026, 10, 3, 9, 30);
        slaService.recordResponseIfNeeded(workOrder, first);
        slaService.recordResponseIfNeeded(workOrder, LocalDateTime.of(2026, 10, 3, 10, 0));

        assertEquals(first, workOrder.getResponseAt());
        assertTrue(workOrder.getResponseBreached());
    }

    @Test
    void recordResponse_WhenOnTime_ShouldNotBreach() {
        workOrder.setSlaResponseDueAt(LocalDateTime.of(2026, 10, 3, 9, 0));
        slaService.recordResponseIfNeeded(workOrder, LocalDateTime.of(2026, 10, 3, 8, 45));
        assertFalse(workOrder.getResponseBreached());
    }

    @Test
    void recordResolution_ShouldSetOnceAndCalculateBreach() {
        workOrder.setSlaResolutionDueAt(LocalDateTime.of(2026, 10, 3, 12, 0));
        LocalDateTime first = LocalDateTime.of(2026, 10, 3, 11, 0);
        slaService.recordResolutionIfNeeded(workOrder, first);
        slaService.recordResolutionIfNeeded(workOrder, LocalDateTime.of(2026, 10, 3, 13, 0));

        assertEquals(first, workOrder.getResolvedAt());
        assertFalse(workOrder.getResolutionBreached());
    }

    @Test
    void recordResolution_WhenLate_ShouldBreach() {
        workOrder.setSlaResolutionDueAt(LocalDateTime.of(2026, 10, 3, 12, 0));
        slaService.recordResolutionIfNeeded(workOrder, LocalDateTime.of(2026, 10, 3, 13, 0));
        assertTrue(workOrder.getResolutionBreached());
    }

    @Test
    void getWorkOrderSla_WhenOtherTechnician_ShouldNotLeak() {
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, "tech.b@keystone.com"))
                .thenThrow(new ResourceNotFoundException("Work order not found with id: 7"));

        assertThrows(ResourceNotFoundException.class, () -> slaService.getWorkOrderSla(7L, "tech.b@keystone.com"));
    }

    @Test
    void getWorkOrderSla_WhenAssignedTechnician_ShouldReturnSummary() {
        workOrder.setSlaPolicyName("Gold");
        workOrder.setSlaResponseDueAt(LocalDateTime.of(2026, 10, 3, 9, 0));
        workOrder.setSlaResolutionDueAt(LocalDateTime.of(2026, 10, 3, 12, 0));
        when(workOrderAccessGuard.requireAccessibleWorkOrder(7L, "tech.a@keystone.com")).thenReturn(workOrder);

        WorkOrderSlaResponse response = slaService.getWorkOrderSla(7L, "tech.a@keystone.com");

        assertEquals(7L, response.getWorkOrderId());
        assertEquals("Gold", response.getSlaPolicyName());
        assertNotNull(response.getSlaStatus());
    }
}
