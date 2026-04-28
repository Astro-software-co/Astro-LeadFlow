ALTER TABLE chat_messages
    ADD COLUMN sender_type VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN bot_session_id UUID REFERENCES bot_session(id),
    ADD COLUMN intent_detected VARCHAR(80),
    ADD COLUMN engine_used VARCHAR(20);

ALTER TABLE chat_messages
    DROP CONSTRAINT IF EXISTS chk_chat_message_sender_type;

ALTER TABLE chat_messages
    ADD CONSTRAINT chk_chat_message_sender_type
    CHECK (sender_type IN ('USER', 'BOT', 'SELLER', 'SYSTEM'));

CREATE INDEX idx_chat_message_bot_session ON chat_messages(bot_session_id);
