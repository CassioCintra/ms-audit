package io.github.cassio.ms_audit.adapter.in.web.response;

import io.github.cassio.ms_audit.domain.audit.AuditEvent;
import io.github.cassio.ms_audit.domain.audit.AuditSource;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        AuditSource source,
        String action,
        String actorId,
        String resourceId,
        Map<String, Object> payload,
        Instant occurredAt
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getSource(),
                event.getAction(),
                event.getActorId(),
                event.getResourceId(),
                event.getPayload(),
                event.getOccurredAt()
        );
    }
}
