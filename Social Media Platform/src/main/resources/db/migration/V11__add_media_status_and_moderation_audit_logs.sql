ALTER TABLE media
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'VISIBLE';

CREATE TABLE IF NOT EXISTS moderation_audit_logs (
    id BINARY(16) NOT NULL,
    actor_id BINARY(16) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_moderation_audit_logs_actor_id (actor_id),
    KEY idx_moderation_audit_logs_target (target_type, target_id),
    CONSTRAINT fk_moderation_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id)
);
