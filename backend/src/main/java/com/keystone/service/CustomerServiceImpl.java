package com.keystone.service;

import com.keystone.dto.CreateCustomerRequest;
import com.keystone.dto.CustomerPageResponse;
import com.keystone.dto.CustomerResponse;
import com.keystone.dto.UpdateCustomerRequest;
import com.keystone.entity.Customer;
import com.keystone.entity.SlaPolicy;
import com.keystone.enums.CustomerStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.repository.SlaPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "createdAt", "updatedAt", "customerCode", "companyName", "email", "city", "status"
    );

    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final SlaPolicyRepository slaPolicyRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerPageResponse getCustomers(int page, int size, String sort, String search, CustomerStatus status) {
        Pageable pageable = buildPageable(page, size, sort);
        String searchTerm = normalizeSearch(search);

        Page<Customer> customerPage = customerRepository.searchCustomers(searchTerm, status, pageable);

        List<CustomerResponse> content = customerPage.getContent()
                .stream()
                .map(CustomerResponse::fromEntity)
                .toList();

        return CustomerPageResponse.builder()
                .content(content)
                .page(customerPage.getNumber())
                .size(customerPage.getSize())
                .totalElements(customerPage.getTotalElements())
                .totalPages(customerPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return CustomerResponse.fromEntity(findCustomer(id));
    }

    @Override
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        String customerCode = normalizeCode(request.getCustomerCode());
        String email = normalizeEmail(request.getEmail());

        assertCustomerCodeAvailable(customerCode, null);
        assertEmailAvailable(email, null);

        Customer customer = Customer.builder()
                .customerCode(customerCode)
                .companyName(trimToNull(request.getCompanyName()))
                .contactFirstName(trimToNull(request.getContactFirstName()))
                .contactLastName(trimToNull(request.getContactLastName()))
                .email(email)
                .phone(trimToNull(request.getPhone()))
                .alternatePhone(trimToNull(request.getAlternatePhone()))
                .addressLine1(trimToNull(request.getAddressLine1()))
                .addressLine2(trimToNull(request.getAddressLine2()))
                .city(trimToNull(request.getCity()))
                .state(trimToNull(request.getState()))
                .postalCode(trimToNull(request.getPostalCode()))
                .country(trimToNull(request.getCountry()))
                .notes(trimToNull(request.getNotes()))
                .status(CustomerStatus.ACTIVE)
                .slaPolicy(resolveSlaPolicy(request.getSlaPolicyId()))
                .build();

        return CustomerResponse.fromEntity(saveAndReload(customer));
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = findCustomer(id);

        String customerCode = normalizeCode(request.getCustomerCode());
        String email = normalizeEmail(request.getEmail());

        assertCustomerCodeAvailable(customerCode, id);
        assertEmailAvailable(email, id);

        customer.setCustomerCode(customerCode);
        customer.setCompanyName(trimToNull(request.getCompanyName()));
        customer.setContactFirstName(trimToNull(request.getContactFirstName()));
        customer.setContactLastName(trimToNull(request.getContactLastName()));
        customer.setEmail(email);
        customer.setPhone(trimToNull(request.getPhone()));
        customer.setAlternatePhone(trimToNull(request.getAlternatePhone()));
        customer.setAddressLine1(trimToNull(request.getAddressLine1()));
        customer.setAddressLine2(trimToNull(request.getAddressLine2()));
        customer.setCity(trimToNull(request.getCity()));
        customer.setState(trimToNull(request.getState()));
        customer.setPostalCode(trimToNull(request.getPostalCode()));
        customer.setCountry(trimToNull(request.getCountry()));
        customer.setNotes(trimToNull(request.getNotes()));
        customer.setSlaPolicy(resolveSlaPolicy(request.getSlaPolicyId()));

        return CustomerResponse.fromEntity(saveAndReload(customer));
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomerStatus(Long id, CustomerStatus status) {
        if (status == null) {
            throw new ApiException("Status is required", HttpStatus.BAD_REQUEST);
        }

        Customer customer = findCustomer(id);
        customer.setStatus(status);
        return CustomerResponse.fromEntity(saveAndReload(customer));
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = findCustomer(id);

        if (siteRepository.existsByCustomerId(id)) {
            throw new ApiException(
                    "Cannot delete customer because one or more sites are associated with it.",
                    HttpStatus.CONFLICT
            );
        }

        customerRepository.delete(customer);
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findByIdWithSlaPolicy(id)
                .or(() -> customerRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    private Customer saveAndReload(Customer customer) {
        Customer saved = customerRepository.save(customer);
        return customerRepository.findByIdWithSlaPolicy(saved.getId()).orElse(saved);
    }

    private SlaPolicy resolveSlaPolicy(Long slaPolicyId) {
        if (slaPolicyId == null) {
            return null;
        }
        return slaPolicyRepository.findById(slaPolicyId)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy not found with id: " + slaPolicyId));
    }

    private void assertCustomerCodeAvailable(String customerCode, Long currentId) {
        boolean exists = currentId == null
                ? customerRepository.existsByCustomerCode(customerCode)
                : customerRepository.existsByCustomerCodeAndIdNot(customerCode, currentId);

        if (exists) {
            throw new DuplicateResourceException("A customer with code " + customerCode + " already exists.");
        }
    }

    private void assertEmailAvailable(String email, Long currentId) {
        if (email == null) {
            return;
        }

        boolean exists = currentId == null
                ? customerRepository.existsByEmail(email)
                : customerRepository.existsByEmailAndIdNot(email, currentId);

        if (exists) {
            throw new DuplicateResourceException("A customer with email address " + email + " already exists.");
        }
    }

    private Pageable buildPageable(int page, int size, String sort) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);
        String sortProperty = (sort != null && SORTABLE_FIELDS.contains(sort.trim())) ? sort.trim() : "createdAt";
        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, sortProperty));
    }

    private String normalizeSearch(String search) {
        return (search != null && !search.isBlank()) ? search.trim() : null;
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
