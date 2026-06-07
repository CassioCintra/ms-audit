package io.github.cassio.ms_audit.adapter.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenRevokedEventDto(
        String action,
        String workspaceId,
        String tokenId,
        String tokenName,
        String revokedBy,
        Instant revokedAt
) {}
