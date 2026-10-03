package com.keystone.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSiteRequest {

    @NotBlank(message = "Site code is required")
    @Size(max = 50, message = "Site code must be at most 50 characters")
    private String siteCode;

    @NotBlank(message = "Site name is required")
    @Size(max = 200, message = "Site name must be at most 200 characters")
    private String siteName;

    @NotNull(message = "Customer is required")
    private Long customerId;

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

    @Size(max = 150, message = "Contact name must be at most 150 characters")
    private String contactName;

    @Size(max = 30, message = "Contact phone must be at most 30 characters")
    private String contactPhone;

    @Email(message = "Please provide a valid contact email address")
    @Size(max = 255, message = "Contact email must be at most 255 characters")
    private String contactEmail;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;
}
