package com.keystone.dto;

import com.keystone.enums.WorkOrderPriority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSlaPolicyRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private WorkOrderPriority priority;

    @NotNull(message = "Response time is required")
    @Min(value = 1, message = "Response time must be greater than 0")
    private Integer responseTimeMinutes;

    @NotNull(message = "Resolution time is required")
    @Min(value = 1, message = "Resolution time must be greater than 0")
    private Integer resolutionTimeMinutes;

    @NotNull(message = "Active flag is required")
    private Boolean active;

    @AssertTrue(message = "Resolution time must be greater than or equal to response time")
    public boolean isDurationValid() {
        if (responseTimeMinutes == null || resolutionTimeMinutes == null) {
            return true;
        }
        return resolutionTimeMinutes >= responseTimeMinutes;
    }
}
