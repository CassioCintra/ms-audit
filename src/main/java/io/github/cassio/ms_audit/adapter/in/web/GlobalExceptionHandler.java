package io.github.cassio.ms_audit.adapter.in.web;

import io.github.cassio.ms_audit.adapter.in.web.response.ErrorResponse;
import io.github.cassio.ms_audit.domain.exception.AuditEventNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuditEventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AuditEventNotFoundException ex, HttpServletRequest request) {
        log.warn("Audit event not found [uri={}, message={}]", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        log.warn("Malformed request [uri={}, message={}]", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "The request body or parameters are malformed.", request.getRequestURI()));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Throwable ex, HttpServletRequest request) {
        if (ex instanceof org.springframework.web.ErrorResponse error && error.getStatusCode().is4xxClientError()) {
            var status = HttpStatus.valueOf(error.getStatusCode().value());
            log.warn("Request rejected [uri={}, status={}]", request.getRequestURI(), status.value());
            return ResponseEntity
                    .status(status)
                    .body(ErrorResponse.of(status, error.getBody().getDetail(), request.getRequestURI()));
        }
        log.error("Unexpected error [uri={}]", request.getRequestURI(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred.", request.getRequestURI()));
    }
}
