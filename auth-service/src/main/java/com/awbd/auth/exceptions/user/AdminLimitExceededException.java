package com.awbd.auth.exceptions.user;

public class AdminLimitExceededException extends RuntimeException {
    public AdminLimitExceededException(String message) {
        super(message);
    }
}
