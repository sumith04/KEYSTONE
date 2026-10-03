package com.keystone.dto;

import com.keystone.entity.SlaPolicy;
import com.keystone.enums.WorkOrderPriority;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaPolicyResponse {

    private Long id;
    private String name;
    private String description;
    private WorkOrderPriority priority;
    private Integer responseTimeMinutes;
    private Integer resolutionTimeMinutes;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SlaPolicyResponse fromEntity(SlaPolicy policy) {
        if (policy == null) {
            return null;
        }
        return SlaPolicyResponse.builder()
                .id(policy.getId())
                .name(policy.getName())
                .description(policy.getDescription())
                .priority(policy.getPriority())
                .responseTimeMinutes(policy.getResponseTimeMinutes())
                .resolutionTimeMinutes(policy.getResolutionTimeMinutes())
                .active(policy.isActive())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
