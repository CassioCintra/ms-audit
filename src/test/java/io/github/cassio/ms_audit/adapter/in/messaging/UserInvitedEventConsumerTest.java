package io.github.cassio.ms_audit.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.cassio.ms_audit.adapter.in.messaging.dto.UserInvitedEventDto;
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
class UserInvitedEventConsumerTest {

    @Mock
    private SaveAuditEventUseCase saveAuditEventUseCase;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private UserInvitedEventConsumer consumer;

    @Test
    void shouldSaveAuditEventWithCorrectFieldsWhenUserInvitedEventReceived() {
        Instant invitedAt = Instant.parse("2026-01-15T10:00:00Z");
        UserInvitedEventDto event = new UserInvitedEventDto(
                "INVITED", "ws-123", "new@user.com", "EDITOR", "admin@co.com", invitedAt);

        when(saveAuditEventUseCase.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(saveAuditEventUseCase).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo(AuditSource.USER_INVITED);
        assertThat(saved.getAction()).isEqualTo("INVITED");
        assertThat(saved.getActorId()).isEqualTo("admin@co.com");
        assertThat(saved.getResourceId()).isEqualTo("new@user.com");
        assertThat(saved.getOccurredAt()).isEqualTo(invitedAt);
    }

    @Test
    void shouldFallbackToNowWhenInvitedAtIsNull() {
        UserInvitedEventDto event = new UserInvitedEventDto(
                "INVITED", "ws-123", "new@user.com", "VIEWER", "admin@co.com", null);

        when(saveAuditEventUseCase.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(saveAuditEventUseCase).save(captor.capture());
        assertThat(captor.getValue().getOccurredAt()).isNotNull();
    }
}
