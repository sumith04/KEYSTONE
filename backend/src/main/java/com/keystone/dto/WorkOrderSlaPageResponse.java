package com.keystone.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderSlaPageResponse {

    private List<WorkOrderSlaResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
