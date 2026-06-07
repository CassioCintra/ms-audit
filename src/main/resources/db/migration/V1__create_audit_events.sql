CREATE TABLE audit_events (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    source       VARCHAR(32) NOT NULL,
    action       VARCHAR(32) NOT NULL,
    actor_id     VARCHAR(255),
    resource_id  VARCHAR(255),
    payload      JSONB       NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_events_occurred_at ON audit_events (occurred_at DESC);
CREATE INDEX idx_audit_events_source      ON audit_events (source);
CREATE INDEX idx_audit_events_action      ON audit_events (action);
CREATE INDEX idx_audit_events_actor_id    ON audit_events (actor_id);
