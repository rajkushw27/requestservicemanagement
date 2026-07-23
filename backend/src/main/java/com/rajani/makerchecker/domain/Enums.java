package com.rajani.makerchecker.domain;

public class Enums {

    public enum RequestStatus { DRAFT, PENDING, APPROVED, REJECTED, EXPIRED, RECALLED }

    public enum Operation { CREATE, UPDATE, DELETE }

    public enum DecisionType { APPROVE, REJECT, REQUEST_CHANGES }

    public enum ApprovalMode { SEQUENTIAL, PARALLEL }

    public enum TimeoutAction { EXPIRE, ESCALATE, AUTO_REJECT }

    private Enums() {}
}
