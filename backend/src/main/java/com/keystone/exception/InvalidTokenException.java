package com.keystone.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends ApiException {

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
