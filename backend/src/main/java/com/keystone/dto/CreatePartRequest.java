package com.keystone.dto;

import com.keystone.enums.PartStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePartRequest {

    @NotBlank(message = "Part number is required")
    @Size(max = 50, message = "Part number must be at most 50 characters")
    private String partNumber;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;

    @Size(max = 4000, message = "Description must be at most 4000 characters")
    private String description;

    @Size(max = 100, message = "Category must be at most 100 characters")
    private String category;

    @Size(max = 30, message = "Unit of measure must be at most 30 characters")
    private String unitOfMeasure;

    @NotNull(message = "Unit cost is required")
    @DecimalMin(value = "0.00", message = "Unit cost must be at least 0")
    private BigDecimal unitCost;

    @NotNull(message = "Quantity in stock is required")
    @Min(value = 0, message = "Quantity in stock must be at least 0")
    private Integer quantityInStock;

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message = "Reorder level must be at least 0")
    private Integer reorderLevel;

    private PartStatus status;
}
