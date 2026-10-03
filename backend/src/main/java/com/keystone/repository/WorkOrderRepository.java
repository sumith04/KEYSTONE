package com.keystone.repository;

import com.keystone.entity.WorkOrder;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    boolean existsBySiteId(Long siteId);

    boolean existsByCustomerId(Long customerId);

    boolean existsBySlaPolicyId(Long slaPolicyId);

    @EntityGraph(attributePaths = {"customer", "site", "assignedTechnician", "createdBy", "slaPolicy"})
    @Query("SELECT w FROM WorkOrder w WHERE w.id = :id")
    Optional<WorkOrder> findByIdWithRelations(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "site", "assignedTechnician", "createdBy", "slaPolicy"})
    @Query("SELECT w FROM WorkOrder w WHERE w.workOrderNumber = :workOrderNumber")
    Optional<WorkOrder> findByWorkOrderNumberWithRelations(@Param("workOrderNumber") String workOrderNumber);

    @EntityGraph(attributePaths = {"customer", "site", "assignedTechnician", "createdBy", "slaPolicy"})
    @Query("SELECT w FROM WorkOrder w WHERE " +
           "(:search IS NULL OR LOWER(w.workOrderNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(w.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(w.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR w.status = :status) AND " +
           "(:priority IS NULL OR w.priority = :priority) AND " +
           "(:customerId IS NULL OR w.customer.id = :customerId) AND " +
           "(:siteId IS NULL OR w.site.id = :siteId) AND " +
           "(:technicianId IS NULL OR w.assignedTechnician.id = :technicianId)")
    long countByAssignedTechnicianIdAndStatus(Long technicianId, WorkOrderStatus status);

    long countByStatus(WorkOrderStatus status);

    Page<WorkOrder> searchWorkOrders(
            @Param("search") String search,
            @Param("status") WorkOrderStatus status,
            @Param("priority") WorkOrderPriority priority,
            @Param("customerId") Long customerId,
            @Param("siteId") Long siteId,
            @Param("technicianId") Long technicianId,
            Pageable pageable
    );
}
