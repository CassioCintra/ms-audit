package io.github.cassio.ms_audit.adapter.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlagEventDto(
        String flagName,
        String serviceName,
        String type,
        Integer rollout,
        Map<String, Boolean> environments,
        List<String> tags,
        String owner,
        LocalDate expiresAt,
        boolean enabled,
        String action,
        String actorId
) {}
