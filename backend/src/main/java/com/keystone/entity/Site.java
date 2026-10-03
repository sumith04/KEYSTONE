package com.keystone.entity;

import com.keystone.enums.SiteStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "sites",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sites_site_code", columnNames = "site_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_code", nullable = false, unique = true)
    private String siteCode;

    @Column(name = "site_name", nullable = false)
    private String siteName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

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

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private SiteStatus status = SiteStatus.ACTIVE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.siteCode != null) {
            this.siteCode = this.siteCode.trim().toUpperCase();
        }
        if (this.contactEmail != null) {
            this.contactEmail = this.contactEmail.trim().toLowerCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.siteCode != null) {
            this.siteCode = this.siteCode.trim().toUpperCase();
        }
        if (this.contactEmail != null) {
            this.contactEmail = this.contactEmail.trim().toLowerCase();
        }
    }
}
