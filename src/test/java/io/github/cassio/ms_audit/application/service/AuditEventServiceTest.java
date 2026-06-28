package io.github.cassio.ms_audit.application.service;

import io.github.cassio.ms_audit.application.port.in.QueryAuditEventsUseCase.AuditQuery;
import io.github.cassio.ms_audit.application.port.out.AuditEventRepository;
import io.github.cassio.ms_audit.domain.audit.AuditEvent;
import io.github.cassio.ms_audit.domain.audit.AuditSource;
import io.github.cassio.ms_audit.domain.exception.AuditEventNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    @Mock
    private AuditEventRepository repository;

    @InjectMocks
    private AuditEventService service;

    private AuditEvent event(UUID id, AuditSource source, String action, String actorId, String resourceId) {
        return AuditEvent.builder()
                .id(id)
                .source(source)
                .action(action)
                .actorId(actorId)
                .resourceId(resourceId)
                .payload(Map.of("key", "value"))
                .occurredAt(Instant.now())
                .build();
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    void shouldDelegateToRepositoryAndReturnSavedEvent() {
        UUID id = UUID.randomUUID();
        AuditEvent input = event(id, AuditSource.FLAG_EVENT, "CREATED", "user-1", "my-flag");
        when(repository.save(any())).thenReturn(input);

        AuditEvent result = service.save(input);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(input);
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getSource()).isEqualTo(AuditSource.FLAG_EVENT);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void shouldPassAllFiltersToRepositoryAndReturnPage() {
        AuditEvent ev = event(UUID.randomUUID(), AuditSource.USER_INVITED, "INVITED", "admin", "user@test.com");
        Page<AuditEvent> page = new PageImpl<>(List.of(ev));
        when(repository.findAll(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        AuditQuery query = new AuditQuery(AuditSource.USER_INVITED, "INVITED", "admin", "user@test.com", null, null);
        Page<AuditEvent> result = service.findAll(query, Pageable.unpaged());

        verify(repository).findAll(AuditSource.USER_INVITED, "INVITED", "admin", "user@test.com", null, null, Pageable.unpaged());
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSource()).isEqualTo(AuditSource.USER_INVITED);
    }

    @Test
    void shouldReturnEmptyPageWhenNoEventsMatch() {
        when(repository.findAll(any(), any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        Page<AuditEvent> result = service.findAll(new AuditQuery(null, null, null, null, null, null), Pageable.unpaged());

        assertThat(result).isEmpty();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void shouldReturnEventWhenFound() {
        UUID id = UUID.randomUUID();
        AuditEvent ev = event(id, AuditSource.TOKEN_REVOKED, "REVOKED", "admin", "token-abc");
        when(repository.findById(id)).thenReturn(Optional.of(ev));

        AuditEvent result = service.findById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getSource()).isEqualTo(AuditSource.TOKEN_REVOKED);
    }

    @Test
    void shouldThrowWhenEventNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(AuditEventNotFoundException.class);
    }
}
