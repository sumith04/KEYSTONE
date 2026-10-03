package com.keystone.service;

import com.keystone.dto.CreatePartRequest;
import com.keystone.dto.PartPageResponse;
import com.keystone.dto.PartResponse;
import com.keystone.dto.UpdatePartRequest;
import com.keystone.entity.Part;
import com.keystone.entity.User;
import com.keystone.enums.PartStatus;
import com.keystone.enums.Role;
import com.keystone.exception.ApiException;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.PartRepository;
import com.keystone.repository.WorkOrderPartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PartServiceImpl implements PartService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "partNumber", "name", "category", "unitCost", "quantityInStock",
            "reorderLevel", "status", "createdAt", "updatedAt"
    );

    private final PartRepository partRepository;
    private final WorkOrderPartRepository workOrderPartRepository;
    private final WorkOrderAccessGuard workOrderAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public PartPageResponse getParts(int page, int size, String sort, String search, String category, PartStatus status) {
        Pageable pageable = buildPageable(page, size, sort);
        Page<Part> partPage = partRepository.searchParts(
                normalizeSearch(search),
                normalizeSearch(category),
                status,
                pageable
        );

        List<PartResponse> content = partPage.getContent()
                .stream()
                .map(PartResponse::fromEntity)
                .toList();

        return PartPageResponse.builder()
                .content(content)
                .page(partPage.getNumber())
                .size(partPage.getSize())
                .totalElements(partPage.getTotalElements())
                .totalPages(partPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PartResponse getPartById(Long id) {
        return PartResponse.fromEntity(findPart(id));
    }

    @Override
    @Transactional
    public PartResponse createPart(CreatePartRequest request, String currentUsername) {
        assertCatalogWritable(currentUsername);

        String partNumber = normalizePartNumber(request.getPartNumber());
        assertPartNumberAvailable(partNumber, null);
        assertNonNegativeInventory(request.getUnitCost(), request.getQuantityInStock(), request.getReorderLevel());

        Part part = Part.builder()
                .partNumber(partNumber)
                .name(trimToNull(request.getName()))
                .description(trimToNull(request.getDescription()))
                .category(trimToNull(request.getCategory()))
                .unitOfMeasure(trimToNull(request.getUnitOfMeasure()))
                .unitCost(scaleMoney(request.getUnitCost()))
                .quantityInStock(request.getQuantityInStock())
                .reorderLevel(request.getReorderLevel())
                .status(request.getStatus() != null ? request.getStatus() : PartStatus.ACTIVE)
                .build();

        return PartResponse.fromEntity(partRepository.save(part));
    }

    @Override
    @Transactional
    public PartResponse updatePart(Long id, UpdatePartRequest request, String currentUsername) {
        assertCatalogWritable(currentUsername);

        Part part = findPart(id);
        String partNumber = normalizePartNumber(request.getPartNumber());
        assertPartNumberAvailable(partNumber, id);
        assertNonNegativeInventory(request.getUnitCost(), request.getQuantityInStock(), request.getReorderLevel());

        part.setPartNumber(partNumber);
        part.setName(trimToNull(request.getName()));
        part.setDescription(trimToNull(request.getDescription()));
        part.setCategory(trimToNull(request.getCategory()));
        part.setUnitOfMeasure(trimToNull(request.getUnitOfMeasure()));
        part.setUnitCost(scaleMoney(request.getUnitCost()));
        part.setQuantityInStock(request.getQuantityInStock());
        part.setReorderLevel(request.getReorderLevel());
        part.setStatus(request.getStatus());

        return PartResponse.fromEntity(partRepository.save(part));
    }

    @Override
    @Transactional
    public void deletePart(Long id, String currentUsername) {
        assertCatalogWritable(currentUsername);

        Part part = findPart(id);
        if (workOrderPartRepository.existsByPartId(id)) {
            throw new ApiException(
                    "Cannot delete part because it has been used on one or more work orders.",
                    HttpStatus.CONFLICT
            );
        }
        partRepository.delete(part);
    }

    private void assertCatalogWritable(String currentUsername) {
        User currentUser = workOrderAccessGuard.requireCurrentUser(currentUsername);
        if (currentUser.getRole() == Role.TECHNICIAN) {
            throw new ApiException(
                    "Technicians cannot manage the global part catalog.",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private void assertPartNumberAvailable(String partNumber, Long currentId) {
        boolean exists = currentId == null
                ? partRepository.existsByPartNumber(partNumber)
                : partRepository.existsByPartNumberAndIdNot(partNumber, currentId);
        if (exists) {
            throw new DuplicateResourceException("A part with number " + partNumber + " already exists.");
        }
    }

    private void assertNonNegativeInventory(BigDecimal unitCost, Integer quantityInStock, Integer reorderLevel) {
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("Unit cost must be at least 0.", HttpStatus.BAD_REQUEST);
        }
        if (quantityInStock == null || quantityInStock < 0) {
            throw new ApiException("Quantity in stock must be at least 0.", HttpStatus.BAD_REQUEST);
        }
        if (reorderLevel == null || reorderLevel < 0) {
            throw new ApiException("Reorder level must be at least 0.", HttpStatus.BAD_REQUEST);
        }
    }

    private Part findPart(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found with id: " + id));
    }

    private Pageable buildPageable(int page, int size, String sort) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);

        Sort.Direction direction = Sort.Direction.ASC;
        String property = "name";

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String candidate = parts[0].trim();
            if (SORTABLE_FIELDS.contains(candidate)) {
                property = candidate;
            }
            if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.DESC;
            } else if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.ASC;
            }
        }

        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, property));
    }

    private String normalizePartNumber(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizeSearch(String search) {
        return (search != null && !search.isBlank()) ? search.trim() : null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
