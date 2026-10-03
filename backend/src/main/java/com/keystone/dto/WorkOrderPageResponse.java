package com.keystone.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderPageResponse {

    private List<WorkOrderResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
