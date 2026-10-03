package com.keystone.entity;

import com.keystone.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", nullable = false, unique = true)
    private String customerCode;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "contact_first_name")
    private String contactFirstName;

    @Column(name = "contact_last_name")
    private String contactLastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "alternate_phone")
    private String alternatePhone;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.customerCode != null) {
            this.customerCode = this.customerCode.trim().toUpperCase();
        }
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.customerCode != null) {
            this.customerCode = this.customerCode.trim().toUpperCase();
        }
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }
}
