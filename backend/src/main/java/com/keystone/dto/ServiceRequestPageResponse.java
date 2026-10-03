package com.keystone.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequestPageResponse {

    private List<ServiceRequestResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
