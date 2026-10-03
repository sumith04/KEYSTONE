package com.keystone.dto;

import com.keystone.entity.TimeLog;
import com.keystone.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeLogResponse {

    private Long id;
    private Long workOrderId;
    private Long technicianId;
    private String technicianName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TimeLogResponse fromEntity(TimeLog timeLog) {
        if (timeLog == null) {
            return null;
        }
        User technician = timeLog.getTechnician();
        return TimeLogResponse.builder()
                .id(timeLog.getId())
                .workOrderId(timeLog.getWorkOrder() != null ? timeLog.getWorkOrder().getId() : null)
                .technicianId(technician != null ? technician.getId() : null)
                .technicianName(formatUserName(technician))
                .startTime(timeLog.getStartTime())
                .endTime(timeLog.getEndTime())
                .durationMinutes(timeLog.getDurationMinutes())
                .notes(timeLog.getNotes())
                .createdAt(timeLog.getCreatedAt())
                .updatedAt(timeLog.getUpdatedAt())
                .build();
    }

    private static String formatUserName(User user) {
        if (user == null) {
            return null;
        }
        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getUserEmail() : fullName;
    }
}
