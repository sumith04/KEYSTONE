package com.keystone.repository;

import com.keystone.entity.ServiceRequest;
import com.keystone.enums.ServiceRequestStatus;
import com.keystone.enums.WorkOrderPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    @EntityGraph(attributePaths = {"customer", "site", "workOrder"})
    @Query("SELECT r FROM ServiceRequest r WHERE r.id = :id")
    Optional<ServiceRequest> findByIdWithRelations(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "site", "workOrder"})
    @Query("SELECT r FROM ServiceRequest r WHERE r.id = :id AND r.customer.id = :customerId")
    Optional<ServiceRequest> findByIdAndCustomerIdWithRelations(
            @Param("id") Long id,
            @Param("customerId") Long customerId
    );

    Optional<ServiceRequest> findByWorkOrderId(Long workOrderId);

    @EntityGraph(attributePaths = {"customer", "site", "workOrder"})
    @Query("SELECT r FROM ServiceRequest r WHERE r.workOrder.id = :workOrderId")
    Optional<ServiceRequest> findByWorkOrderIdWithRelations(@Param("workOrderId") Long workOrderId);

    long countByCustomerIdAndStatus(Long customerId, ServiceRequestStatus status);

    @EntityGraph(attributePaths = {"customer", "site", "workOrder"})
    @Query("SELECT r FROM ServiceRequest r WHERE r.customer.id = :customerId AND " +
           "(:search IS NULL OR LOWER(r.requestNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:priority IS NULL OR r.priority = :priority)")
    Page<ServiceRequest> searchForCustomer(
            @Param("customerId") Long customerId,
            @Param("search") String search,
            @Param("status") ServiceRequestStatus status,
            @Param("priority") WorkOrderPriority priority,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "site", "workOrder"})
    @Query("SELECT r FROM ServiceRequest r WHERE " +
           "(:search IS NULL OR LOWER(r.requestNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:priority IS NULL OR r.priority = :priority) AND " +
           "(:customerId IS NULL OR r.customer.id = :customerId)")
    Page<ServiceRequest> searchAll(
            @Param("search") String search,
            @Param("status") ServiceRequestStatus status,
            @Param("priority") WorkOrderPriority priority,
            @Param("customerId") Long customerId,
            Pageable pageable
    );
}
