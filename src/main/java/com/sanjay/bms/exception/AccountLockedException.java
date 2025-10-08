package com.sanjay.bms.exception;

class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
