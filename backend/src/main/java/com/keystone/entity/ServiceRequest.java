package com.keystone.entity;

import com.keystone.enums.ServiceRequestStatus;
import com.keystone.enums.WorkOrderPriority;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "service_requests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_service_requests_number", columnNames = "request_number")
        },
        indexes = {
                @Index(name = "idx_service_requests_customer", columnList = "customer_id"),
                @Index(name = "idx_service_requests_status", columnList = "status"),
                @Index(name = "idx_service_requests_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", unique = true)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private WorkOrderPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ServiceRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "preferred_date")
    private LocalDateTime preferredDate;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", unique = true)
    private WorkOrder workOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.requestedAt == null) {
            this.requestedAt = now;
        }
        if (this.status == null) {
            this.status = ServiceRequestStatus.SUBMITTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
