package io.github.cassio.ms_audit.adapter.in.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp
) {
    private static final String BASE_TYPE_URI = "https://problems.audit.io/";

    public static ErrorResponse of(HttpStatus httpStatus, String detail, String instance) {
        return new ErrorResponse(
                BASE_TYPE_URI + httpStatus.name().toLowerCase().replace('_', '-'),
                httpStatus.getReasonPhrase(),
                httpStatus.value(),
                detail,
                instance,
                Instant.now()
        );
    }
}
