package com.researchoffice.shared.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base for expected business failures; mapped to RFC 9457 problem details with a stable code. */
@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AppException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static AppException notFound(String what, Object id) {
        return new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " " + id + " not found");
    }

    public static AppException rule(String code, String message) {
        return new AppException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }
}
