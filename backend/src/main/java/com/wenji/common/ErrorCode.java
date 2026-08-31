package com.wenji.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST(40000, HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(40101, HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(40100, HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, HttpStatus.NOT_FOUND),
    USERNAME_EXISTS(40901, HttpStatus.CONFLICT),
    CONFLICT(40900, HttpStatus.CONFLICT),
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final HttpStatus status;

    ErrorCode(int code, HttpStatus status) {
        this.code = code;
        this.status = status;
    }

    public int code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}

