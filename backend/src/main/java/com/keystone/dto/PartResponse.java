package com.keystone.dto;

import com.keystone.entity.Part;
import com.keystone.enums.PartStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartResponse {

    private Long id;
    private String partNumber;
    private String name;
    private String description;
    private String category;
    private String unitOfMeasure;
    private BigDecimal unitCost;
    private Integer quantityInStock;
    private Integer reorderLevel;
    private PartStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PartResponse fromEntity(Part part) {
        if (part == null) {
            return null;
        }
        return PartResponse.builder()
                .id(part.getId())
                .partNumber(part.getPartNumber())
                .name(part.getName())
                .description(part.getDescription())
                .category(part.getCategory())
                .unitOfMeasure(part.getUnitOfMeasure())
                .unitCost(part.getUnitCost())
                .quantityInStock(part.getQuantityInStock())
                .reorderLevel(part.getReorderLevel())
                .status(part.getStatus())
                .createdAt(part.getCreatedAt())
                .updatedAt(part.getUpdatedAt())
                .build();
    }
}
