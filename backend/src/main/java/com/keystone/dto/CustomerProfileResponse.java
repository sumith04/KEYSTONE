package com.keystone.dto;

import com.keystone.entity.Customer;
import com.keystone.enums.CustomerStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfileResponse {

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

    public static CustomerProfileResponse fromEntity(Customer customer) {
        if (customer == null) {
            return null;
        }
        return CustomerProfileResponse.builder()
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
                .build();
    }
}
