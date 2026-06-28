package io.github.cassio.ms_audit.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cassio.ms_audit.adapter.in.messaging.dto.UserInvitedEventDto;
import io.github.cassio.ms_audit.application.port.in.SaveAuditEventUseCase;
import io.github.cassio.ms_audit.domain.audit.AuditEvent;
import io.github.cassio.ms_audit.domain.audit.AuditSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserInvitedEventConsumer {

    private final SaveAuditEventUseCase saveAuditEventUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${audit.kafka.topics.user-invited}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(UserInvitedEventDto event) {
        log.info("Received user invited event [workspaceId={}, inviteeEmail={}, invitedBy={}]",
                event.workspaceId(), event.inviteeEmail(), event.invitedBy());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
            saveAuditEventUseCase.save(AuditEvent.builder()
                    .id(UUID.randomUUID())
                    .source(AuditSource.USER_INVITED)
                    .action(event.action())
                    .actorId(event.invitedBy())
                    .resourceId(event.inviteeEmail())
                    .payload(payload)
                    .occurredAt(event.invitedAt() != null ? event.invitedAt() : Instant.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to process user invited event [workspaceId={}, inviteeEmail={}]",
                    event.workspaceId(), event.inviteeEmail(), e);
        }
    }
}
