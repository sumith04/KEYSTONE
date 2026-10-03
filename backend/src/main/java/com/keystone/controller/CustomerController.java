package com.keystone.controller;

import com.keystone.dto.CreateCustomerRequest;
import com.keystone.dto.CustomerPageResponse;
import com.keystone.dto.CustomerResponse;
import com.keystone.dto.UpdateCustomerRequest;
import com.keystone.enums.CustomerStatus;
import com.keystone.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_CUSTOMER')")
    public ResponseEntity<CustomerPageResponse> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CustomerStatus status) {

        CustomerPageResponse response = customerService.getCustomers(page, size, sort, search, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_CUSTOMER')")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @PostMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'CREATE_CUSTOMER')")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_CUSTOMER')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_CUSTOMER')")
    public ResponseEntity<CustomerResponse> updateCustomerStatus(
            @PathVariable Long id,
            @RequestParam CustomerStatus status) {
        return ResponseEntity.ok(customerService.updateCustomerStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'DELETE_CUSTOMER')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
