package io.github.cassio.ms_audit.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.cassio.ms_audit.adapter.in.messaging.dto.TokenRevokedEventDto;
import io.github.cassio.ms_audit.application.port.in.SaveAuditEventUseCase;
import io.github.cassio.ms_audit.domain.audit.AuditEvent;
import io.github.cassio.ms_audit.domain.audit.AuditSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRevokedEventConsumerTest {

    @Mock
    private SaveAuditEventUseCase saveAuditEventUseCase;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private TokenRevokedEventConsumer consumer;

    @Test
    void shouldSaveAuditEventWithCorrectFieldsWhenTokenRevokedEventReceived() {
        Instant revokedAt = Instant.parse("2026-03-10T14:30:00Z");
        TokenRevokedEventDto event = new TokenRevokedEventDto(
                "REVOKED", "ws-456", "token-abc", "CI Token", "admin@co.com", revokedAt);

        when(saveAuditEventUseCase.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(saveAuditEventUseCase).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo(AuditSource.TOKEN_REVOKED);
        assertThat(saved.getAction()).isEqualTo("REVOKED");
        assertThat(saved.getActorId()).isEqualTo("admin@co.com");
        assertThat(saved.getResourceId()).isEqualTo("token-abc");
        assertThat(saved.getOccurredAt()).isEqualTo(revokedAt);
    }

    @Test
    void shouldFallbackToNowWhenRevokedAtIsNull() {
        TokenRevokedEventDto event = new TokenRevokedEventDto(
                "REVOKED", "ws-456", "token-xyz", "Deploy Token", "admin@co.com", null);

        when(saveAuditEventUseCase.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(saveAuditEventUseCase).save(captor.capture());
        assertThat(captor.getValue().getOccurredAt()).isNotNull();
    }
}
