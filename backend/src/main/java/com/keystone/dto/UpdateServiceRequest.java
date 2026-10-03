package com.keystone.dto;

import com.keystone.enums.WorkOrderPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateServiceRequest {

    @NotNull(message = "Site is required")
    private Long siteId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 4000, message = "Description must be at most 4000 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private WorkOrderPriority priority;

    private LocalDateTime preferredDate;

    @Size(max = 120, message = "Contact name must be at most 120 characters")
    private String contactName;

    @Size(max = 40, message = "Contact phone must be at most 40 characters")
    private String contactPhone;
}
