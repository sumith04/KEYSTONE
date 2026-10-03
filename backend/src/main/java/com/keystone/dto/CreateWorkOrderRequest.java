package com.keystone.dto;

import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkType;
import jakarta.validation.constraints.AssertTrue;
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
public class CreateWorkOrderRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @Size(max = 4000, message = "Description must be at most 4000 characters")
    private String description;

    @NotNull(message = "Customer is required")
    private Long customerId;

    @NotNull(message = "Site is required")
    private Long siteId;

    @NotNull(message = "Priority is required")
    private WorkOrderPriority priority;

    @NotNull(message = "Work type is required")
    private WorkType workType;

    private LocalDateTime scheduledStart;

    private LocalDateTime scheduledEnd;

    @Size(max = 4000, message = "Notes must be at most 4000 characters")
    private String notes;

    @AssertTrue(message = "Scheduled end must not be before scheduled start")
    public boolean isScheduleValid() {
        if (scheduledStart == null || scheduledEnd == null) {
            return true;
        }
        return !scheduledEnd.isBefore(scheduledStart);
    }
}
