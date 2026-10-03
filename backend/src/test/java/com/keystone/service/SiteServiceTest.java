package com.keystone.service;

import com.keystone.dto.CreateSiteRequest;
import com.keystone.dto.SitePageResponse;
import com.keystone.dto.SiteResponse;
import com.keystone.dto.UpdateSiteRequest;
import com.keystone.entity.Customer;
import com.keystone.entity.Site;
import com.keystone.enums.CustomerStatus;
import com.keystone.enums.SiteStatus;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.exception.ApiException;
import com.keystone.repository.CustomerRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @InjectMocks
    private SiteServiceImpl siteService;

    private Customer customer;
    private Site hq;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(10L)
                .customerCode("ACME001")
                .companyName("Acme Facilities")
                .status(CustomerStatus.ACTIVE)
                .build();

        hq = Site.builder()
                .id(5L)
                .siteCode("HQ-01")
                .siteName("Headquarters")
                .customer(customer)
                .city("Austin")
                .contactEmail("hq@acme.com")
                .status(SiteStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createSite_WithValidData_ShouldSaveNormalizedSite() {
        CreateSiteRequest request = CreateSiteRequest.builder()
                .siteCode(" hq-01 ")
                .siteName("Headquarters")
                .customerId(10L)
                .contactEmail("HQ@Acme.com")
                .city("Austin")
                .build();

        when(siteRepository.existsBySiteCode("HQ-01")).thenReturn(false);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(siteRepository.save(any(Site.class))).thenReturn(hq);
        when(siteRepository.findByIdWithCustomer(5L)).thenReturn(Optional.of(hq));

        SiteResponse response = siteService.createSite(request);

        ArgumentCaptor<Site> captor = ArgumentCaptor.forClass(Site.class);
        verify(siteRepository).save(captor.capture());
        assertEquals("HQ-01", captor.getValue().getSiteCode());
        assertEquals("hq@acme.com", captor.getValue().getContactEmail());
        assertEquals(10L, captor.getValue().getCustomer().getId());
        assertEquals("ACME001", response.getCustomerCode());
        assertEquals("Acme Facilities", response.getCustomerName());
    }

    @Test
    void createSite_WithDuplicateCode_ShouldThrowConflict() {
        CreateSiteRequest request = CreateSiteRequest.builder()
                .siteCode("HQ-01")
                .siteName("Headquarters")
                .customerId(10L)
                .build();

        when(siteRepository.existsBySiteCode("HQ-01")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> siteService.createSite(request));
        verify(siteRepository, never()).save(any());
    }

    @Test
    void createSite_WhenCustomerMissing_ShouldThrowNotFound() {
        CreateSiteRequest request = CreateSiteRequest.builder()
                .siteCode("WH-02")
                .siteName("Warehouse")
                .customerId(99L)
                .build();

        when(siteRepository.existsBySiteCode("WH-02")).thenReturn(false);
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> siteService.createSite(request));
    }

    @Test
    void getSiteById_WhenExists_ShouldReturnMappedCustomerFields() {
        when(siteRepository.findByIdWithCustomer(5L)).thenReturn(Optional.of(hq));

        SiteResponse response = siteService.getSiteById(5L);

        assertEquals("HQ-01", response.getSiteCode());
        assertEquals(10L, response.getCustomerId());
        assertEquals("ACME001", response.getCustomerCode());
        assertEquals("Acme Facilities", response.getCustomerName());
    }

    @Test
    void getSiteById_WhenMissing_ShouldThrowNotFound() {
        when(siteRepository.findByIdWithCustomer(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> siteService.getSiteById(99L));
    }

    @Test
    void getSites_ShouldFilterBySearchStatusAndCustomer() {
        when(siteRepository.searchSites(eq("hq"), eq(SiteStatus.ACTIVE), eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(hq)));

        SitePageResponse response = siteService.getSites(0, 10, "createdAt", "hq", SiteStatus.ACTIVE, 10L);

        assertEquals(1, response.getContent().size());
        assertEquals("HQ-01", response.getContent().get(0).getSiteCode());
        assertEquals(0, response.getPage());
    }

    @Test
    void updateSite_ShouldReassignCustomerAndPersist() {
        Customer other = Customer.builder()
                .id(11L)
                .customerCode("NORTH01")
                .companyName("North Campus")
                .status(CustomerStatus.ACTIVE)
                .build();

        UpdateSiteRequest request = UpdateSiteRequest.builder()
                .siteCode("HQ-01")
                .siteName("Main Campus")
                .customerId(11L)
                .city("Dallas")
                .build();

        when(siteRepository.findByIdWithCustomer(5L)).thenReturn(Optional.of(hq));
        when(siteRepository.existsBySiteCodeAndIdNot("HQ-01", 5L)).thenReturn(false);
        when(customerRepository.findById(11L)).thenReturn(Optional.of(other));
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SiteResponse response = siteService.updateSite(5L, request);

        assertEquals("Main Campus", response.getSiteName());
        assertEquals(11L, response.getCustomerId());
        assertEquals("NORTH01", response.getCustomerCode());
    }

    @Test
    void updateSiteStatus_ShouldChangeStatus() {
        when(siteRepository.findByIdWithCustomer(5L)).thenReturn(Optional.of(hq));
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SiteResponse response = siteService.updateSiteStatus(5L, SiteStatus.INACTIVE);

        assertEquals(SiteStatus.INACTIVE, response.getStatus());
    }

    @Test
    void deleteSite_ShouldRemoveSite() {
        when(siteRepository.findByIdWithCustomer(5L)).thenReturn(Optional.of(hq));
        when(workOrderRepository.existsBySiteId(5L)).thenReturn(false);

        siteService.deleteSite(5L);

        verify(siteRepository).delete(hq);
    }

    @Test
    void deleteSite_WhenWorkOrdersExist_ShouldThrowConflict() {
        when(siteRepository.findByIdWithCustomer(5L)).thenReturn(Optional.of(hq));
        when(workOrderRepository.existsBySiteId(5L)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> siteService.deleteSite(5L));
        assertTrue(exception.getMessage().contains("work orders are associated"));
        verify(siteRepository, never()).delete(any());
    }
}
