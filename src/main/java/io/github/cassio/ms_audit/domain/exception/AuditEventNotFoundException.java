package io.github.cassio.ms_audit.domain.exception;

import java.util.UUID;

public class AuditEventNotFoundException extends RuntimeException {

    private AuditEventNotFoundException(String message) {
        super(message);
    }

    public static AuditEventNotFoundException notFound(UUID id) {
        return new AuditEventNotFoundException("Audit event not found: " + id);
    }
}
