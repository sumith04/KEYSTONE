package com.keystone.service;

import com.keystone.dto.CreateWorkOrderPartRequest;
import com.keystone.dto.UpdateWorkOrderPartRequest;
import com.keystone.dto.WorkOrderPartResponse;
import com.keystone.entity.Part;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.entity.WorkOrderPart;
import com.keystone.enums.PartStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.PartRepository;
import com.keystone.repository.WorkOrderPartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderPartServiceImpl implements WorkOrderPartService {

    private final WorkOrderPartRepository workOrderPartRepository;
    private final PartRepository partRepository;
    private final WorkOrderAccessGuard workOrderAccessGuard;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderPartResponse> getWorkOrderParts(Long workOrderId, String currentUsername) {
        workOrderAccessGuard.requireAccessibleWorkOrder(workOrderId, currentUsername);
        return workOrderPartRepository.findByWorkOrderIdWithRelations(workOrderId)
                .stream()
                .map(WorkOrderPartResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public WorkOrderPartResponse addWorkOrderPart(
            Long workOrderId,
            CreateWorkOrderPartRequest request,
            String currentUsername) {

        User currentUser = workOrderAccessGuard.requireCurrentUser(currentUsername);
        WorkOrder workOrder = workOrderAccessGuard.requireAccessibleWorkOrder(workOrderId, currentUsername);
        workOrderAccessGuard.assertPartsMutable(workOrder);

        int quantity = requirePositiveQuantity(request.getQuantity());
        Part part = lockActivePart(request.getPartId());
        consumeStock(part, quantity);

        BigDecimal unitCost = scaleMoney(part.getUnitCost());
        WorkOrderPart usage = WorkOrderPart.builder()
                .workOrder(workOrder)
                .part(part)
                .quantityUsed(quantity)
                .unitCostAtUsage(unitCost)
                .totalCost(calculateTotal(unitCost, quantity))
                .usedBy(currentUser)
                .usedAt(LocalDateTime.now())
                .build();

        WorkOrderPart saved = workOrderPartRepository.save(usage);
        return WorkOrderPartResponse.fromEntity(
                workOrderPartRepository.findByIdAndWorkOrderIdWithRelations(saved.getId(), workOrderId)
                        .orElse(saved)
        );
    }

    @Override
    @Transactional
    public WorkOrderPartResponse updateWorkOrderPart(
            Long workOrderId,
            Long workOrderPartId,
            UpdateWorkOrderPartRequest request,
            String currentUsername) {

        WorkOrder workOrder = workOrderAccessGuard.requireAccessibleWorkOrder(workOrderId, currentUsername);
        workOrderAccessGuard.assertPartsMutable(workOrder);

        WorkOrderPart usage = findUsage(workOrderId, workOrderPartId);
        int newQuantity = requirePositiveQuantity(request.getQuantity());
        int previousQuantity = usage.getQuantityUsed();
        int delta = newQuantity - previousQuantity;

        Part part = lockPart(usage.getPart().getId());
        if (delta > 0) {
            consumeStock(part, delta);
        } else if (delta < 0) {
            restoreStock(part, -delta);
        }

        usage.setQuantityUsed(newQuantity);
        usage.setTotalCost(calculateTotal(usage.getUnitCostAtUsage(), newQuantity));

        workOrderPartRepository.save(usage);
        return WorkOrderPartResponse.fromEntity(
                workOrderPartRepository.findByIdAndWorkOrderIdWithRelations(workOrderPartId, workOrderId)
                        .orElse(usage)
        );
    }

    @Override
    @Transactional
    public void deleteWorkOrderPart(Long workOrderId, Long workOrderPartId, String currentUsername) {
        WorkOrder workOrder = workOrderAccessGuard.requireAccessibleWorkOrder(workOrderId, currentUsername);
        workOrderAccessGuard.assertPartsMutable(workOrder);

        WorkOrderPart usage = findUsage(workOrderId, workOrderPartId);
        Part part = lockPart(usage.getPart().getId());
        restoreStock(part, usage.getQuantityUsed());
        workOrderPartRepository.delete(usage);
    }

    private WorkOrderPart findUsage(Long workOrderId, Long workOrderPartId) {
        return workOrderPartRepository.findByIdAndWorkOrderIdWithRelations(workOrderPartId, workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Work order part not found with id: " + workOrderPartId));
    }

    private Part lockActivePart(Long partId) {
        Part part = lockPart(partId);
        if (part.getStatus() != PartStatus.ACTIVE) {
            throw new ApiException("Only active parts can be used on a work order.", HttpStatus.CONFLICT);
        }
        return part;
    }

    private Part lockPart(Long partId) {
        return partRepository.findByIdForUpdate(partId)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found with id: " + partId));
    }

    private void consumeStock(Part part, int quantity) {
        int available = part.getQuantityInStock() == null ? 0 : part.getQuantityInStock();
        if (quantity > available) {
            throw new ApiException(
                    "Insufficient stock for part " + part.getPartNumber() + ". Available: " + available + ".",
                    HttpStatus.CONFLICT
            );
        }
        int remaining = available - quantity;
        if (remaining < 0) {
            throw new ApiException("Stock cannot become negative.", HttpStatus.CONFLICT);
        }
        part.setQuantityInStock(remaining);
        partRepository.save(part);
        try {
            notificationService.notifyLowStockIfNeeded(part);
        } catch (Exception ignored) {
            // Low-stock alerts must not block parts usage.
        }
    }

    private void restoreStock(Part part, int quantity) {
        int current = part.getQuantityInStock() == null ? 0 : part.getQuantityInStock();
        part.setQuantityInStock(current + quantity);
        partRepository.save(part);
    }

    private int requirePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ApiException("Quantity must be greater than 0.", HttpStatus.BAD_REQUEST);
        }
        return quantity;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotal(BigDecimal unitCost, int quantity) {
        return unitCost.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}
