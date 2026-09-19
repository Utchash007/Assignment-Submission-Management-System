package com.asms.springasms.exception;

public class NotFoundException extends RuntimeException {

    private final String title;

    public NotFoundException(String message) {
        this("Resource Not Found", message);
    }

    public NotFoundException(String title, String message) {
        super(message);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
