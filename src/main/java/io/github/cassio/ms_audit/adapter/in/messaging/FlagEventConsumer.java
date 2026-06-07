package io.github.cassio.ms_audit.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cassio.ms_audit.adapter.in.messaging.dto.FlagEventDto;
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
public class FlagEventConsumer {

    private final SaveAuditEventUseCase saveAuditEventUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${audit.kafka.topics.flag-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(FlagEventDto event) {
        log.info("Received flag event [action={}, flagName={}, actorId={}]",
                event.action(), event.flagName(), event.actorId());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
            saveAuditEventUseCase.save(AuditEvent.builder()
                    .id(UUID.randomUUID())
                    .source(AuditSource.FLAG_EVENT)
                    .action(event.action())
                    .actorId(event.actorId())
                    .resourceId(event.flagName())
                    .payload(payload)
                    .occurredAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to process flag event [action={}, flagName={}]",
                    event.action(), event.flagName(), e);
        }
    }
}
