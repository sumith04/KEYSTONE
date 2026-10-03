package com.keystone.dto;

import com.keystone.entity.Customer;
import com.keystone.entity.Site;
import com.keystone.enums.SiteStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteResponse {

    private Long id;
    private String siteCode;
    private String siteName;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private SiteStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SiteResponse fromEntity(Site site) {
        if (site == null) {
            return null;
        }

        Customer customer = site.getCustomer();
        return SiteResponse.builder()
                .id(site.getId())
                .siteCode(site.getSiteCode())
                .siteName(site.getSiteName())
                .customerId(customer != null ? customer.getId() : null)
                .customerCode(customer != null ? customer.getCustomerCode() : null)
                .customerName(customer != null ? customer.getCompanyName() : null)
                .addressLine1(site.getAddressLine1())
                .addressLine2(site.getAddressLine2())
                .city(site.getCity())
                .state(site.getState())
                .postalCode(site.getPostalCode())
                .country(site.getCountry())
                .contactName(site.getContactName())
                .contactPhone(site.getContactPhone())
                .contactEmail(site.getContactEmail())
                .status(site.getStatus())
                .description(site.getDescription())
                .createdAt(site.getCreatedAt())
                .updatedAt(site.getUpdatedAt())
                .build();
    }
}
