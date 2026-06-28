package io.github.cassio.ms_audit.adapter.in.web;

import io.github.cassio.ms_audit.adapter.in.web.config.SecurityConfig;
import io.github.cassio.ms_audit.adapter.in.web.filter.CorrelatorFilter;
import io.github.cassio.ms_audit.application.port.in.QueryAuditEventsUseCase;
import io.github.cassio.ms_audit.domain.audit.AuditEvent;
import io.github.cassio.ms_audit.domain.audit.AuditSource;
import io.github.cassio.ms_audit.domain.exception.AuditEventNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditEventController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CorrelatorFilter.class})
class AuditEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryAuditEventsUseCase useCase;

    private AuditEvent event(UUID id, AuditSource source, String action, String actorId, String resourceId) {
        return AuditEvent.builder()
                .id(id)
                .source(source)
                .action(action)
                .actorId(actorId)
                .resourceId(resourceId)
                .payload(Map.of("key", "value"))
                .occurredAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    // ── GET /events ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldListEventsAndReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        when(useCase.findAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event(id, AuditSource.FLAG_EVENT, "CREATED", "user-1", "my-flag"))));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].source").value("FLAG_EVENT"))
                .andExpect(jsonPath("$.content[0].action").value("CREATED"))
                .andExpect(jsonPath("$.content[0].actorId").value("user-1"))
                .andExpect(jsonPath("$.content[0].resourceId").value("my-flag"))
                .andExpect(header().exists(CorrelatorFilter.HEADER));
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyPageWhenNoEventsMatch() throws Exception {
        when(useCase.findAll(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/events").param("action", "DELETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ── GET /events/{id} ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldReturnEventByIdAndReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        when(useCase.findById(id)).thenReturn(event(id, AuditSource.USER_INVITED, "INVITED", "admin", "user@test.com"));

        mockMvc.perform(get("/events/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.source").value("USER_INVITED"))
                .andExpect(jsonPath("$.action").value("INVITED"));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenEventNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(useCase.findById(id)).thenThrow(AuditEventNotFoundException.notFound(id));

        mockMvc.perform(get("/events/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── Correlator ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldReuseCorrelatorFromRequestHeader() throws Exception {
        when(useCase.findAll(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/events").header(CorrelatorFilter.HEADER, "my-correlator"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelatorFilter.HEADER, "my-correlator"));
    }
}
