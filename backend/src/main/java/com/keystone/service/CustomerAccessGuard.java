package com.keystone.service;

import com.keystone.entity.Customer;
import com.keystone.entity.User;
import com.keystone.enums.Role;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CustomerAccessGuard {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public User requireCurrentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ApiException("Authenticated user could not be resolved.", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByUserEmail(username.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }

    @Transactional(readOnly = true)
    public Customer requireLinkedCustomer(String username) {
        User user = requireCurrentUser(username);
        if (user.getRole() != Role.CUSTOMER) {
            throw new ApiException("Customer portal access requires the CUSTOMER role.", HttpStatus.FORBIDDEN);
        }
        if (user.getCustomer() != null && user.getCustomer().getId() != null) {
            return customerRepository.findByIdWithSlaPolicy(user.getCustomer().getId())
                    .or(() -> customerRepository.findById(user.getCustomer().getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found."));
        }
        return customerRepository.findByEmail(user.getUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No customer profile is linked to this account."));
    }
}
