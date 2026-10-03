package com.keystone.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignWorkOrderRequest {

    @NotNull(message = "Technician is required")
    private Long technicianId;
}
