package com.keystone.repository;

import com.keystone.entity.TimeLog;
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
}
