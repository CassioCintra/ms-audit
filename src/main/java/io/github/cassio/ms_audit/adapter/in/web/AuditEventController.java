package io.github.cassio.ms_audit.adapter.in.web;

import io.github.cassio.ms_audit.adapter.in.web.response.AuditEventResponse;
import io.github.cassio.ms_audit.application.port.in.QueryAuditEventsUseCase;
import io.github.cassio.ms_audit.application.port.in.QueryAuditEventsUseCase.AuditQuery;
import io.github.cassio.ms_audit.domain.audit.AuditSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class AuditEventController {

    private final QueryAuditEventsUseCase queryAuditEventsUseCase;

    public AuditEventController(QueryAuditEventsUseCase queryAuditEventsUseCase) {
        this.queryAuditEventsUseCase = queryAuditEventsUseCase;
    }

    @GetMapping
    public Page<AuditEventResponse> listEvents(
            @RequestParam(required = false) AuditSource source,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "occurredAt") Pageable pageable) {
        return queryAuditEventsUseCase
                .findAll(new AuditQuery(source, action, actorId, resourceId, from, to), pageable)
                .map(AuditEventResponse::from);
    }

    @GetMapping("/{id}")
    public AuditEventResponse getEvent(@PathVariable UUID id) {
        return AuditEventResponse.from(queryAuditEventsUseCase.findById(id));
    }
}
