package com.keystone.repository;

import com.keystone.entity.WorkOrderPart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderPartRepository extends JpaRepository<WorkOrderPart, Long> {

    boolean existsByPartId(Long partId);

    boolean existsByWorkOrderId(Long workOrderId);

    Optional<WorkOrderPart> findByWorkOrderIdAndPartId(Long workOrderId, Long partId);

    @EntityGraph(attributePaths = {"part", "usedBy", "workOrder"})
    @Query("SELECT wop FROM WorkOrderPart wop WHERE wop.workOrder.id = :workOrderId ORDER BY wop.usedAt DESC")
    List<WorkOrderPart> findByWorkOrderIdWithRelations(@Param("workOrderId") Long workOrderId);

    @EntityGraph(attributePaths = {"part", "usedBy", "workOrder"})
    @Query("SELECT wop FROM WorkOrderPart wop WHERE wop.id = :id AND wop.workOrder.id = :workOrderId")
    Optional<WorkOrderPart> findByIdAndWorkOrderIdWithRelations(
            @Param("id") Long id,
            @Param("workOrderId") Long workOrderId
    );
}
