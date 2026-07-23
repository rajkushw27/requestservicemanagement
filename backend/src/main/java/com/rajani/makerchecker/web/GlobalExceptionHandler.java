package com.rajani.makerchecker.web;

import com.rajani.makerchecker.service.ApprovalException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApprovalException.class)
    public ResponseEntity<Dtos.ApiError> handle(ApprovalException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new Dtos.ApiError(e.getStatus().name(), e.getMessage()));
    }
}
