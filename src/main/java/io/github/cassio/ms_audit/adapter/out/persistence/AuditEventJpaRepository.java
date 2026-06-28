package io.github.cassio.ms_audit.adapter.out.persistence;

import io.github.cassio.ms_audit.domain.audit.AuditSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, UUID> {

    @Query("""
            SELECT e FROM AuditEventEntity e
            WHERE (:source IS NULL OR e.source = :source)
              AND (:action IS NULL OR e.action = :action)
              AND (:actorId IS NULL OR e.actorId = :actorId)
              AND (:resourceId IS NULL OR e.resourceId = :resourceId)
              AND (:from IS NULL OR e.occurredAt >= :from)
              AND (:to IS NULL OR e.occurredAt <= :to)
            ORDER BY e.occurredAt DESC
            """)
    Page<AuditEventEntity> findWithFilters(
            @Param("source") AuditSource source,
            @Param("action") String action,
            @Param("actorId") String actorId,
            @Param("resourceId") String resourceId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
