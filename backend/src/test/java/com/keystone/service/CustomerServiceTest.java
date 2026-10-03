package com.keystone.service;

import com.keystone.dto.CreateCustomerRequest;
import com.keystone.dto.CustomerPageResponse;
import com.keystone.dto.CustomerResponse;
import com.keystone.dto.UpdateCustomerRequest;
import com.keystone.entity.Customer;
import com.keystone.enums.CustomerStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SiteRepository;
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
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer acme;

    @BeforeEach
    void setUp() {
        acme = Customer.builder()
                .id(1L)
                .customerCode("ACME001")
                .companyName("Acme Facilities")
                .contactFirstName("Jane")
                .contactLastName("Doe")
                .email("jane.doe@acme.com")
                .phone("555-0100")
                .city("Austin")
                .status(CustomerStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createCustomer_WithValidData_ShouldSaveNormalizedCustomer() {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .customerCode(" acme001 ")
                .companyName("Acme Facilities")
                .email("Jane.Doe@Acme.com")
                .phone("555-0100")
                .city("Austin")
                .build();

        when(customerRepository.existsByCustomerCode("ACME001")).thenReturn(false);
        when(customerRepository.existsByEmail("jane.doe@acme.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(acme);

        CustomerResponse response = customerService.createCustomer(request);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertEquals("ACME001", captor.getValue().getCustomerCode());
        assertEquals("jane.doe@acme.com", captor.getValue().getEmail());
        assertEquals(CustomerStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals("ACME001", response.getCustomerCode());
    }

    @Test
    void createCustomer_WithDuplicateCode_ShouldThrowConflict() {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .customerCode("ACME001")
                .companyName("Acme Facilities")
                .build();

        when(customerRepository.existsByCustomerCode("ACME001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> customerService.createCustomer(request));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void createCustomer_WithDuplicateEmail_ShouldThrowConflict() {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .customerCode("ACME002")
                .companyName("Acme East")
                .email("jane.doe@acme.com")
                .build();

        when(customerRepository.existsByCustomerCode("ACME002")).thenReturn(false);
        when(customerRepository.existsByEmail("jane.doe@acme.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> customerService.createCustomer(request));
    }

    @Test
    void getCustomerById_WhenExists_ShouldReturnCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(acme));

        CustomerResponse response = customerService.getCustomerById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Acme Facilities", response.getCompanyName());
    }

    @Test
    void getCustomerById_WhenMissing_ShouldThrowNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomerById(99L));
    }

    @Test
    void getCustomers_ShouldReturnPagedSearchResults() {
        when(customerRepository.searchCustomers(eq("acme"), eq(CustomerStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(acme)));

        CustomerPageResponse response = customerService.getCustomers(0, 10, "createdAt", "acme", CustomerStatus.ACTIVE);

        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
        assertEquals(0, response.getPage());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    void updateCustomer_ShouldPersistChangesAndRecheckUniqueness() {
        UpdateCustomerRequest request = UpdateCustomerRequest.builder()
                .customerCode("ACME001")
                .companyName("Acme Facilities LLC")
                .email("billing@acme.com")
                .city("Dallas")
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(acme));
        when(customerRepository.existsByCustomerCodeAndIdNot("ACME001", 1L)).thenReturn(false);
        when(customerRepository.existsByEmailAndIdNot("billing@acme.com", 1L)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.updateCustomer(1L, request);

        assertEquals("Acme Facilities LLC", response.getCompanyName());
        assertEquals("billing@acme.com", response.getEmail());
        assertEquals("Dallas", response.getCity());
    }

    @Test
    void updateCustomerStatus_ShouldChangeStatus() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(acme));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.updateCustomerStatus(1L, CustomerStatus.INACTIVE);

        assertEquals(CustomerStatus.INACTIVE, response.getStatus());
    }

    @Test
    void deleteCustomer_WhenSitesExist_ShouldThrowConflict() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(acme));
        when(siteRepository.existsByCustomerId(1L)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> customerService.deleteCustomer(1L));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getMessage().contains("sites are associated"));
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void deleteCustomer_WhenNoSites_ShouldDelete() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(acme));
        when(siteRepository.existsByCustomerId(1L)).thenReturn(false);

        customerService.deleteCustomer(1L);

        verify(customerRepository).delete(acme);
    }
}
