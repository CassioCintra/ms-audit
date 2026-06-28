package io.github.cassio.ms_audit.application.service;

import io.github.cassio.ms_audit.application.port.in.QueryAuditEventsUseCase;
import io.github.cassio.ms_audit.application.port.in.SaveAuditEventUseCase;
import io.github.cassio.ms_audit.application.port.out.AuditEventRepository;
import io.github.cassio.ms_audit.domain.audit.AuditEvent;
import io.github.cassio.ms_audit.domain.exception.AuditEventNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventService implements SaveAuditEventUseCase, QueryAuditEventsUseCase {

    private final AuditEventRepository repository;

    @Override
    public AuditEvent save(AuditEvent event) {
        log.debug("Saving audit event [source={}, action={}, actorId={}, resourceId={}]",
                event.getSource(), event.getAction(), event.getActorId(), event.getResourceId());
        AuditEvent saved = repository.save(event);
        log.info("Audit event saved [id={}, source={}, action={}]",
                saved.getId(), saved.getSource(), saved.getAction());
        return saved;
    }

    @Override
    public Page<AuditEvent> findAll(AuditQuery query, Pageable pageable) {
        log.debug("Querying audit events [source={}, action={}, actorId={}, resourceId={}, from={}, to={}]",
                query.source(), query.action(), query.actorId(), query.resourceId(), query.from(), query.to());
        return repository.findAll(
                query.source(), query.action(), query.actorId(),
                query.resourceId(), query.from(), query.to(), pageable);
    }

    @Override
    public AuditEvent findById(UUID id) {
        log.debug("Finding audit event [id={}]", id);
        return repository.findById(id)
                .orElseThrow(() -> AuditEventNotFoundException.notFound(id));
    }
}
