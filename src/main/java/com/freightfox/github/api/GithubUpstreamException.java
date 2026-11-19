package com.freightfox.github.api;

import org.springframework.http.HttpStatus;

public class GithubUpstreamException extends RuntimeException {
    private final HttpStatus status;

    public GithubUpstreamException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() { return status; }
}
