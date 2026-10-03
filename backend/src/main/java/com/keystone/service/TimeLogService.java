package com.keystone.service;

import com.keystone.dto.CreateTimeLogRequest;
import com.keystone.dto.TimeLogResponse;
import com.keystone.dto.UpdateTimeLogRequest;

import java.util.List;

public interface TimeLogService {

    List<TimeLogResponse> getTimeLogs(Long workOrderId, String currentUsername);

    TimeLogResponse addTimeLog(Long workOrderId, CreateTimeLogRequest request, String currentUsername);

    TimeLogResponse updateTimeLog(
            Long workOrderId,
            Long timeLogId,
            UpdateTimeLogRequest request,
            String currentUsername
    );

    void deleteTimeLog(Long workOrderId, Long timeLogId, String currentUsername);
}
