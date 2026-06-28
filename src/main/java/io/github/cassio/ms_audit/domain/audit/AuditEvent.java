package io.github.cassio.ms_audit.domain.audit;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class AuditEvent {

    private final UUID id;
    private final AuditSource source;
    private final String action;
    private final String actorId;
    private final String resourceId;
    private final Map<String, Object> payload;
    private final Instant occurredAt;
}
