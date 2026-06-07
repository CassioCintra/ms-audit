package io.github.cassio.ms_audit.application.port.in;

import io.github.cassio.ms_audit.domain.audit.AuditEvent;
import io.github.cassio.ms_audit.domain.audit.AuditSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface QueryAuditEventsUseCase {

    record AuditQuery(
            AuditSource source,
            String action,
            String actorId,
            String resourceId,
            Instant from,
            Instant to
    ) {}

    Page<AuditEvent> findAll(AuditQuery query, Pageable pageable);

    AuditEvent findById(UUID id);
}
