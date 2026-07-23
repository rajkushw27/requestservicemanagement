package com.rajani.makerchecker.service;

import org.springframework.http.HttpStatus;

public class ApprovalException extends RuntimeException {

    private final HttpStatus status;

    private ApprovalException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static ApprovalException badRequest(String message) { return new ApprovalException(HttpStatus.BAD_REQUEST, message); }
    public static ApprovalException forbidden(String message) { return new ApprovalException(HttpStatus.FORBIDDEN, message); }
    public static ApprovalException conflict(String message) { return new ApprovalException(HttpStatus.CONFLICT, message); }
    public static ApprovalException notFound(String message) { return new ApprovalException(HttpStatus.NOT_FOUND, message); }
    public static ApprovalException unauthorized(String message) { return new ApprovalException(HttpStatus.UNAUTHORIZED, message); }
}
