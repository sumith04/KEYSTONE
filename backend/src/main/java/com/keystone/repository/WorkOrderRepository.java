package com.keystone.repository;

import com.keystone.entity.WorkOrder;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.repository.projection.PriorityCountProjection;
import com.keystone.repository.projection.StatusCountProjection;
import com.keystone.repository.projection.TechnicianStatusCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    long countByAssignedTechnicianIdAndStatus(Long technicianId, WorkOrderStatus status);

    long countByStatus(WorkOrderStatus status);

    long countByCustomerIdAndStatus(Long customerId, WorkOrderStatus status);

    @EntityGraph(attributePaths = {"customer", "site", "assignedTechnician", "createdBy", "slaPolicy"})
    @Query("SELECT w FROM WorkOrder w WHERE w.id = :id AND w.customer.id = :customerId")
    Optional<WorkOrder> findByIdAndCustomerIdWithRelations(
            @Param("id") Long id,
            @Param("customerId") Long customerId
    );

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
    Page<WorkOrder> searchWorkOrders(
            @Param("search") String search,
            @Param("status") WorkOrderStatus status,
            @Param("priority") WorkOrderPriority priority,
            @Param("customerId") Long customerId,
            @Param("siteId") Long siteId,
            @Param("technicianId") Long technicianId,
            Pageable pageable
    );

    @Query("SELECT COUNT(w) FROM WorkOrder w WHERE " +
           "(:from IS NULL OR w.createdAt >= :from) AND (:to IS NULL OR w.createdAt <= :to) AND " +
           "(:technicianId IS NULL OR w.assignedTechnician.id = :technicianId)")
    long countCreatedInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @Query("SELECT w.status AS status, COUNT(w) AS total FROM WorkOrder w WHERE " +
           "(:from IS NULL OR w.createdAt >= :from) AND (:to IS NULL OR w.createdAt <= :to) AND " +
           "(:technicianId IS NULL OR w.assignedTechnician.id = :technicianId) " +
           "GROUP BY w.status")
    List<StatusCountProjection> countByStatusInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @Query("SELECT w.priority AS priority, COUNT(w) AS total FROM WorkOrder w WHERE " +
           "(:from IS NULL OR w.createdAt >= :from) AND (:to IS NULL OR w.createdAt <= :to) AND " +
           "(:technicianId IS NULL OR w.assignedTechnician.id = :technicianId) " +
           "GROUP BY w.priority")
    List<PriorityCountProjection> countByPriorityInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @Query("SELECT t.id AS technicianId, t.firstName AS firstName, t.lastName AS lastName, " +
           "w.status AS status, COUNT(w) AS total " +
           "FROM WorkOrder w JOIN w.assignedTechnician t WHERE " +
           "(:from IS NULL OR w.createdAt >= :from) AND (:to IS NULL OR w.createdAt <= :to) AND " +
           "(:technicianId IS NULL OR t.id = :technicianId) " +
           "GROUP BY t.id, t.firstName, t.lastName, w.status")
    List<TechnicianStatusCountProjection> countTechnicianStatusInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @EntityGraph(attributePaths = {"customer", "site", "assignedTechnician", "slaPolicy"})
    @Query("SELECT w FROM WorkOrder w WHERE " +
           "(:from IS NULL OR w.createdAt >= :from) AND (:to IS NULL OR w.createdAt <= :to) AND " +
           "(:technicianId IS NULL OR w.assignedTechnician.id = :technicianId) AND " +
           "(w.slaResponseDueAt IS NOT NULL OR w.slaResolutionDueAt IS NOT NULL)")
    List<WorkOrder> findSlaWorkOrdersInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @EntityGraph(attributePaths = {"customer", "site", "assignedTechnician", "slaPolicy"})
    @Query("SELECT w FROM WorkOrder w WHERE " +
           "(:from IS NULL OR w.createdAt >= :from) AND (:to IS NULL OR w.createdAt <= :to) AND " +
           "(:technicianId IS NULL OR w.assignedTechnician.id = :technicianId) " +
           "ORDER BY w.createdAt DESC")
    List<WorkOrder> findRecentCreatedInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId,
            Pageable pageable
    );

    @Query("SELECT w.createdAt FROM WorkOrder w WHERE " +
           "(:from IS NULL OR w.createdAt >= :from) AND (:to IS NULL OR w.createdAt <= :to) AND " +
           "(:technicianId IS NULL OR w.assignedTechnician.id = :technicianId)")
    List<LocalDateTime> findCreatedAtInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @Query("SELECT w.actualEnd FROM WorkOrder w WHERE w.actualEnd IS NOT NULL AND " +
           "(:from IS NULL OR w.actualEnd >= :from) AND (:to IS NULL OR w.actualEnd <= :to) AND " +
           "(:technicianId IS NULL OR w.assignedTechnician.id = :technicianId)")
    List<LocalDateTime> findActualEndInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @Query("SELECT w.updatedAt FROM WorkOrder w WHERE w.status = com.keystone.enums.WorkOrderStatus.CLOSED AND " +
           "(:from IS NULL OR w.updatedAt >= :from) AND (:to IS NULL OR w.updatedAt <= :to) AND " +
           "(:technicianId IS NULL OR w.assignedTechnician.id = :technicianId)")
    List<LocalDateTime> findClosedUpdatedAtInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @Query("SELECT w.createdAt, w.actualEnd FROM WorkOrder w WHERE w.actualEnd IS NOT NULL AND " +
           "(:from IS NULL OR w.createdAt >= :from) AND (:to IS NULL OR w.createdAt <= :to) AND " +
           "(:technicianId IS NULL OR w.assignedTechnician.id = :technicianId)")
    List<Object[]> findCompletionPairsInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );
}
