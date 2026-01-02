package com.app.exception;

public class AllocationNotAllowedException extends BusinessException {

    public AllocationNotAllowedException(String message) {
        super("ALLOCATION_NOT_ALLOWED", message);
    }
}
