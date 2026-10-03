package com.keystone.service;

import com.keystone.dto.CreateCustomerRequest;
import com.keystone.dto.CustomerPageResponse;
import com.keystone.dto.CustomerResponse;
import com.keystone.dto.UpdateCustomerRequest;
import com.keystone.enums.CustomerStatus;

public interface CustomerService {

    CustomerPageResponse getCustomers(int page, int size, String sort, String search, CustomerStatus status);

    CustomerResponse getCustomerById(Long id);

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request);

    CustomerResponse updateCustomerStatus(Long id, CustomerStatus status);

    void deleteCustomer(Long id);
}
