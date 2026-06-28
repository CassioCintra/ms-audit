package io.github.cassio.ms_audit.adapter.out.persistence;

import io.github.cassio.ms_audit.TestcontainersConfiguration;
import io.github.cassio.ms_audit.domain.audit.AuditEvent;
import io.github.cassio.ms_audit.domain.audit.AuditSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, AuditEventPersistenceAdapter.class})
class AuditEventPersistenceAdapterTest {

    @Autowired
    private AuditEventPersistenceAdapter adapter;

    private AuditEvent newEvent(AuditSource source, String action, String actorId, String resourceId, Instant occurredAt) {
        return AuditEvent.builder()
                .id(UUID.randomUUID())
                .source(source)
                .action(action)
                .actorId(actorId)
                .resourceId(resourceId)
                .payload(Map.of("key", "value"))
                .occurredAt(occurredAt)
                .build();
    }

    // ── save / findById ───────────────────────────────────────────────────────

    @Test
    void shouldSaveAndFindEventById() {
        AuditEvent saved = adapter.save(newEvent(AuditSource.FLAG_EVENT, "CREATED", "user-1", "my-flag", Instant.now()));

        Optional<AuditEvent> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getSource()).isEqualTo(AuditSource.FLAG_EVENT);
        assertThat(found.get().getAction()).isEqualTo("CREATED");
        assertThat(found.get().getActorId()).isEqualTo("user-1");
        assertThat(found.get().getResourceId()).isEqualTo("my-flag");
    }

    @Test
    void shouldReturnEmptyWhenEventNotFound() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }

    // ── findAll filters ───────────────────────────────────────────────────────

    @Test
    void shouldFilterBySource() {
        adapter.save(newEvent(AuditSource.FLAG_EVENT,   "CREATED", "u1", "flag-a", Instant.now()));
        adapter.save(newEvent(AuditSource.USER_INVITED, "INVITED", "u2", "user@x.com", Instant.now()));

        Page<AuditEvent> result = adapter.findAll(AuditSource.FLAG_EVENT, null, null, null, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1)
                .first().extracting(AuditEvent::getSource).isEqualTo(AuditSource.FLAG_EVENT);
    }

    @Test
    void shouldFilterByAction() {
        adapter.save(newEvent(AuditSource.FLAG_EVENT, "CREATED", "u1", "flag-a", Instant.now()));
        adapter.save(newEvent(AuditSource.FLAG_EVENT, "DELETED", "u1", "flag-b", Instant.now()));

        Page<AuditEvent> result = adapter.findAll(null, "DELETED", null, null, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1)
                .first().extracting(AuditEvent::getAction).isEqualTo("DELETED");
    }

    @Test
    void shouldFilterByActorId() {
        adapter.save(newEvent(AuditSource.FLAG_EVENT, "CREATED", "admin", "flag-a", Instant.now()));
        adapter.save(newEvent(AuditSource.FLAG_EVENT, "CREATED", "user-x", "flag-b", Instant.now()));

        Page<AuditEvent> result = adapter.findAll(null, null, "admin", null, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1)
                .first().extracting(AuditEvent::getActorId).isEqualTo("admin");
    }

    @Test
    void shouldFilterByResourceId() {
        adapter.save(newEvent(AuditSource.FLAG_EVENT, "CREATED", "u1", "flag-target", Instant.now()));
        adapter.save(newEvent(AuditSource.FLAG_EVENT, "CREATED", "u1", "flag-other", Instant.now()));

        Page<AuditEvent> result = adapter.findAll(null, null, null, "flag-target", null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1)
                .first().extracting(AuditEvent::getResourceId).isEqualTo("flag-target");
    }

    @Test
    void shouldFilterByFromAndTo() {
        Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        adapter.save(newEvent(AuditSource.FLAG_EVENT, "OLD",    "u1", "r1", base.minus(10, ChronoUnit.DAYS)));
        adapter.save(newEvent(AuditSource.FLAG_EVENT, "RECENT", "u1", "r2", base.minus(1, ChronoUnit.DAYS)));

        Page<AuditEvent> result = adapter.findAll(null, null, null, null,
                base.minus(3, ChronoUnit.DAYS), base, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1)
                .first().extracting(AuditEvent::getAction).isEqualTo("RECENT");
    }

    @Test
    void shouldReturnAllEventsWhenNoFiltersApplied() {
        adapter.save(newEvent(AuditSource.FLAG_EVENT,    "CREATED", "u1", "r1", Instant.now()));
        adapter.save(newEvent(AuditSource.USER_INVITED,  "INVITED", "u2", "r2", Instant.now()));
        adapter.save(newEvent(AuditSource.TOKEN_REVOKED, "REVOKED", "u3", "r3", Instant.now()));

        Page<AuditEvent> result = adapter.findAll(null, null, null, null, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(3);
    }
}
