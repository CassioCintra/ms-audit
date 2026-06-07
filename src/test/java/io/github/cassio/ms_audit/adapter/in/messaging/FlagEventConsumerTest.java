package io.github.cassio.ms_audit.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.cassio.ms_audit.adapter.in.messaging.dto.FlagEventDto;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlagEventConsumerTest {

    @Mock
    private SaveAuditEventUseCase saveAuditEventUseCase;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private FlagEventConsumer consumer;

    @Test
    void shouldSaveAuditEventWithCorrectFieldsWhenFlagEventReceived() {
        FlagEventDto event = new FlagEventDto(
                "my-flag", "billing", "BOOLEAN", null,
                Map.of("prod", true), List.of("tag1"), null, null,
                true, "CREATED", "user-123");

        when(saveAuditEventUseCase.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(saveAuditEventUseCase).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo(AuditSource.FLAG_EVENT);
        assertThat(saved.getAction()).isEqualTo("CREATED");
        assertThat(saved.getActorId()).isEqualTo("user-123");
        assertThat(saved.getResourceId()).isEqualTo("my-flag");
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOccurredAt()).isNotNull();
    }

    @Test
    void shouldSaveAuditEventForDeletedAction() {
        FlagEventDto event = new FlagEventDto(
                "old-flag", "orders", "BOOLEAN", null,
                Map.of(), List.of(), null, null,
                false, "DELETED", "admin-456");

        when(saveAuditEventUseCase.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(saveAuditEventUseCase).save(captor.capture());

        assertThat(captor.getValue().getAction()).isEqualTo("DELETED");
        assertThat(captor.getValue().getActorId()).isEqualTo("admin-456");
        assertThat(captor.getValue().getResourceId()).isEqualTo("old-flag");
    }
}
