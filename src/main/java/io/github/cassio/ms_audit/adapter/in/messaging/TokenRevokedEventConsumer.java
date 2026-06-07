package io.github.cassio.ms_audit.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cassio.ms_audit.adapter.in.messaging.dto.TokenRevokedEventDto;
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
public class TokenRevokedEventConsumer {

    private final SaveAuditEventUseCase saveAuditEventUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${audit.kafka.topics.token-revoked}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(TokenRevokedEventDto event) {
        log.info("Received token revoked event [workspaceId={}, tokenId={}, revokedBy={}]",
                event.workspaceId(), event.tokenId(), event.revokedBy());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
            saveAuditEventUseCase.save(AuditEvent.builder()
                    .id(UUID.randomUUID())
                    .source(AuditSource.TOKEN_REVOKED)
                    .action(event.action())
                    .actorId(event.revokedBy())
                    .resourceId(event.tokenId())
                    .payload(payload)
                    .occurredAt(event.revokedAt() != null ? event.revokedAt() : Instant.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to process token revoked event [workspaceId={}, tokenId={}]",
                    event.workspaceId(), event.tokenId(), e);
        }
    }
}
