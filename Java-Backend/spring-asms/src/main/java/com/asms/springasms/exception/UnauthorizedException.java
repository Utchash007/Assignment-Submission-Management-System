package com.asms.springasms.exception;

public class UnauthorizedException extends RuntimeException {

    private final String title;

    public UnauthorizedException(String message) {
        this("Unauthorized", message);
    }

    public UnauthorizedException(String title, String message) {
        super(message);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
