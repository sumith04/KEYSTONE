package com.keystone.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private String message;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public MessageResponse(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
