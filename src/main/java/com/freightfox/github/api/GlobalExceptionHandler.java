package com.freightfox.github.api;

import com.freightfox.github.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex,
                                         HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct().sorted().collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler({BadRequestException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class})
    ResponseEntity<ApiError> invalidRequest(Exception ex, HttpServletRequest req) {
        String message = ex instanceof HttpMessageNotReadableException
                ? "Invalid JSON request body"
                : ex instanceof MethodArgumentTypeMismatchException
                ? "Invalid query parameter type"
                : ex instanceof HandlerMethodValidationException
                ? "Invalid query parameter"
                : ex.getMessage();
        return error(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(GithubUpstreamException.class)
    ResponseEntity<ApiError> upstream(GithubUpstreamException ex, HttpServletRequest req) {
        return error(ex.status(), ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error at {}", req.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred", req);
    }

    private ResponseEntity<ApiError> error(HttpStatus code, String message, HttpServletRequest req) {
        return ResponseEntity.status(code).body(new ApiError(
                "error", message, Instant.now(), req.getRequestURI()));
    }
}
