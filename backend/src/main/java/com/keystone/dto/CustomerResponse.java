package com.keystone.dto;

import com.keystone.entity.Customer;
import com.keystone.enums.CustomerStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private Long id;
    private String customerCode;
    private String companyName;
    private String contactFirstName;
    private String contactLastName;
    private String email;
    private String phone;
    private String alternatePhone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private CustomerStatus status;
    private String notes;
    private Long slaPolicyId;
    private String slaPolicyName;
    private Boolean slaPolicyActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerResponse fromEntity(Customer customer) {
        if (customer == null) {
            return null;
        }
        return CustomerResponse.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
                .companyName(customer.getCompanyName())
                .contactFirstName(customer.getContactFirstName())
                .contactLastName(customer.getContactLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .alternatePhone(customer.getAlternatePhone())
                .addressLine1(customer.getAddressLine1())
                .addressLine2(customer.getAddressLine2())
                .city(customer.getCity())
                .state(customer.getState())
                .postalCode(customer.getPostalCode())
                .country(customer.getCountry())
                .status(customer.getStatus())
                .notes(customer.getNotes())
                .slaPolicyId(customer.getSlaPolicy() != null ? customer.getSlaPolicy().getId() : null)
                .slaPolicyName(customer.getSlaPolicy() != null ? customer.getSlaPolicy().getName() : null)
                .slaPolicyActive(customer.getSlaPolicy() != null ? customer.getSlaPolicy().isActive() : null)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
