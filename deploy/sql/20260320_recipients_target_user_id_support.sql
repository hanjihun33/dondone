-- Add target_user_id support to existing recipients table in deployed environments.

ALTER TABLE recipients
    ADD COLUMN IF NOT EXISTS target_user_id BIGINT;

ALTER TABLE recipients
    DROP CONSTRAINT IF EXISTS fk_recipients_target_user;

ALTER TABLE recipients
    ADD CONSTRAINT fk_recipients_target_user
    FOREIGN KEY (target_user_id) REFERENCES users(id);

CREATE INDEX IF NOT EXISTS idx_recipients_target_user_id
    ON recipients (target_user_id);
