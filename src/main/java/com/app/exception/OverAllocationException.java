package com.app.exception;

public class OverAllocationException extends BusinessException {

    public OverAllocationException(String message) {
        super("OVER_ALLOCATION", message);
    }
}
