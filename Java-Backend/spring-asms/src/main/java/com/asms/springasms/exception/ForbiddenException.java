package com.asms.springasms.exception;

public class ForbiddenException extends RuntimeException {

    private final String title;

    public ForbiddenException(String message) {
        this("Forbidden", message);
    }

    public ForbiddenException(String title, String message) {
        super(message);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
