package io.github.cassio.ms_audit.application.port.out;

import io.github.cassio.ms_audit.domain.audit.AuditEvent;
import io.github.cassio.ms_audit.domain.audit.AuditSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository {

    AuditEvent save(AuditEvent event);

    Optional<AuditEvent> findById(UUID id);

    Page<AuditEvent> findAll(AuditSource source, String action, String actorId,
                             String resourceId, Instant from, Instant to, Pageable pageable);
}
