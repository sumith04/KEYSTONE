package com.keystone.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianTimeTotalResponse {

    private Long technicianId;
    private String technicianName;
    private long totalLoggedMinutes;
}
