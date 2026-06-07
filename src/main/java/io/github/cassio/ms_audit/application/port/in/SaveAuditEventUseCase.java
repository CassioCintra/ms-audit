package io.github.cassio.ms_audit.application.port.in;

import io.github.cassio.ms_audit.domain.audit.AuditEvent;

public interface SaveAuditEventUseCase {

    AuditEvent save(AuditEvent event);
}
