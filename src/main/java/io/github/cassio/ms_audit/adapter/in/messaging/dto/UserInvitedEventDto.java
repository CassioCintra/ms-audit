package io.github.cassio.ms_audit.adapter.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserInvitedEventDto(
        String action,
        String workspaceId,
        String inviteeEmail,
        String role,
        String invitedBy,
        Instant invitedAt
) {}
