package com.keystone.repository;

import com.keystone.entity.TimeLog;
import com.keystone.repository.projection.TechnicianMinutesProjection;
import com.keystone.repository.projection.WorkOrderMinutesProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    boolean existsByWorkOrderId(Long workOrderId);

    @EntityGraph(attributePaths = {"technician", "workOrder"})
    @Query("SELECT t FROM TimeLog t WHERE t.technician.id = :technicianId ORDER BY t.startTime DESC")
    List<TimeLog> findByTechnicianIdWithRelations(@Param("technicianId") Long technicianId);

    @EntityGraph(attributePaths = {"technician", "workOrder"})
    @Query("SELECT t FROM TimeLog t WHERE t.workOrder.id = :workOrderId ORDER BY t.startTime DESC")
    List<TimeLog> findByWorkOrderIdWithRelations(@Param("workOrderId") Long workOrderId);

    @EntityGraph(attributePaths = {"technician", "workOrder"})
    @Query("SELECT t FROM TimeLog t WHERE t.id = :id AND t.workOrder.id = :workOrderId")
    Optional<TimeLog> findByIdAndWorkOrderIdWithRelations(
            @Param("id") Long id,
            @Param("workOrderId") Long workOrderId
    );

    @Query("SELECT COUNT(t) > 0 FROM TimeLog t WHERE t.technician.id = :technicianId " +
           "AND t.startTime < :endTime AND t.endTime > :startTime " +
           "AND (:excludeId IS NULL OR t.id <> :excludeId)")
    boolean existsOverlappingLog(
            @Param("technicianId") Long technicianId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId
    );

    @Query("SELECT COALESCE(SUM(t.durationMinutes), 0) FROM TimeLog t WHERE " +
           "(:from IS NULL OR t.startTime >= :from) AND (:to IS NULL OR t.startTime <= :to) AND " +
           "(:technicianId IS NULL OR t.technician.id = :technicianId)")
    long sumDurationMinutesInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @Query("SELECT COUNT(t) FROM TimeLog t WHERE " +
           "(:from IS NULL OR t.startTime >= :from) AND (:to IS NULL OR t.startTime <= :to) AND " +
           "(:technicianId IS NULL OR t.technician.id = :technicianId)")
    long countInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @Query("SELECT COUNT(DISTINCT t.technician.id) FROM TimeLog t WHERE " +
           "(:from IS NULL OR t.startTime >= :from) AND (:to IS NULL OR t.startTime <= :to) AND " +
           "(:technicianId IS NULL OR t.technician.id = :technicianId)")
    long countDistinctTechniciansInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @Query("SELECT t.technician.id AS technicianId, t.technician.firstName AS firstName, " +
           "t.technician.lastName AS lastName, COALESCE(SUM(t.durationMinutes), 0) AS totalMinutes " +
           "FROM TimeLog t WHERE " +
           "(:from IS NULL OR t.startTime >= :from) AND (:to IS NULL OR t.startTime <= :to) AND " +
           "(:technicianId IS NULL OR t.technician.id = :technicianId) " +
           "GROUP BY t.technician.id, t.technician.firstName, t.technician.lastName")
    List<TechnicianMinutesProjection> sumMinutesByTechnicianInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );

    @Query("SELECT t.workOrder.id AS workOrderId, t.workOrder.workOrderNumber AS workOrderNumber, " +
           "t.workOrder.title AS title, COALESCE(SUM(t.durationMinutes), 0) AS totalMinutes " +
           "FROM TimeLog t WHERE " +
           "(:from IS NULL OR t.startTime >= :from) AND (:to IS NULL OR t.startTime <= :to) AND " +
           "(:technicianId IS NULL OR t.technician.id = :technicianId) " +
           "GROUP BY t.workOrder.id, t.workOrder.workOrderNumber, t.workOrder.title")
    List<WorkOrderMinutesProjection> sumMinutesByWorkOrderInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("technicianId") Long technicianId
    );
}
