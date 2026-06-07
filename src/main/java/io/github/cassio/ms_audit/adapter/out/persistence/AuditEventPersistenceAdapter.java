package io.github.cassio.ms_audit.adapter.out.persistence;

import io.github.cassio.ms_audit.application.port.out.AuditEventRepository;
import io.github.cassio.ms_audit.domain.audit.AuditEvent;
import io.github.cassio.ms_audit.domain.audit.AuditSource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditEventPersistenceAdapter implements AuditEventRepository {

    private final AuditEventJpaRepository jpaRepository;

    @Override
    public AuditEvent save(AuditEvent event) {
        return jpaRepository.save(AuditEventEntity.from(event)).toDomain();
    }

    @Override
    public Optional<AuditEvent> findById(UUID id) {
        return jpaRepository.findById(id).map(AuditEventEntity::toDomain);
    }

    @Override
    public Page<AuditEvent> findAll(AuditSource source, String action, String actorId,
                                    String resourceId, Instant from, Instant to, Pageable pageable) {
        return jpaRepository.findWithFilters(source, action, actorId, resourceId, from, to, pageable)
                .map(AuditEventEntity::toDomain);
    }
}
