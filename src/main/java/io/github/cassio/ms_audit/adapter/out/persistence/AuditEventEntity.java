package io.github.cassio.ms_audit.adapter.out.persistence;

import io.github.cassio.ms_audit.domain.audit.AuditEvent;
import io.github.cassio.ms_audit.domain.audit.AuditSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditSource source;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "resource_id")
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public static AuditEventEntity from(AuditEvent event) {
        return AuditEventEntity.builder()
                .id(event.getId())
                .source(event.getSource())
                .action(event.getAction())
                .actorId(event.getActorId())
                .resourceId(event.getResourceId())
                .payload(event.getPayload())
                .occurredAt(event.getOccurredAt())
                .build();
    }

    public AuditEvent toDomain() {
        return AuditEvent.builder()
                .id(id)
                .source(source)
                .action(action)
                .actorId(actorId)
                .resourceId(resourceId)
                .payload(payload)
                .occurredAt(occurredAt)
                .build();
    }
}
