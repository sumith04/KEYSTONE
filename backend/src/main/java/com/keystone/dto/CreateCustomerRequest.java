package com.keystone.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCustomerRequest {

    @NotBlank(message = "Customer code is required")
    @Size(max = 50, message = "Customer code must be at most 50 characters")
    private String customerCode;

    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name must be at most 200 characters")
    private String companyName;

    @Size(max = 100, message = "Contact first name must be at most 100 characters")
    private String contactFirstName;

    @Size(max = 100, message = "Contact last name must be at most 100 characters")
    private String contactLastName;

    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @Size(max = 30, message = "Phone must be at most 30 characters")
    private String phone;

    @Size(max = 30, message = "Alternate phone must be at most 30 characters")
    private String alternatePhone;

    @Size(max = 255, message = "Address line 1 must be at most 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must be at most 255 characters")
    private String addressLine2;

    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    @Size(max = 100, message = "State must be at most 100 characters")
    private String state;

    @Size(max = 20, message = "Postal code must be at most 20 characters")
    private String postalCode;

    @Size(max = 100, message = "Country must be at most 100 characters")
    private String country;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;
}
