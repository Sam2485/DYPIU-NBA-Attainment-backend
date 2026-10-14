-- =========================================================================
-- V24: Create Emmu Institutional Messenger, Suggestions & Notifications Schema
-- =========================================================================

-- 1. Direct Conversations
CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(36) PRIMARY KEY,
    user_one_id BIGINT NOT NULL REFERENCES users(id),
    user_two_id BIGINT NOT NULL REFERENCES users(id),
    last_message_preview VARCHAR(500),
    last_message_sender_id BIGINT REFERENCES users(id),
    last_message_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_conversations_user_pair UNIQUE (user_one_id, user_two_id),
    CONSTRAINT ck_conversations_user_order CHECK (user_one_id < user_two_id)
);

CREATE INDEX IF NOT EXISTS idx_conversations_user_one ON conversations(user_one_id);
CREATE INDEX IF NOT EXISTS idx_conversations_user_two ON conversations(user_two_id);
CREATE INDEX IF NOT EXISTS idx_conversations_last_msg ON conversations(last_message_at DESC);

-- 2. Direct Messages (Supports End-to-End Encrypted content & IV)
CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    iv VARCHAR(64),
    is_encrypted BOOLEAN NOT NULL DEFAULT TRUE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_messages_unread ON messages(conversation_id, recipient_id, read_at);

-- 3. Academic Suggestions (Inbox & Outbox)
CREATE TABLE IF NOT EXISTS suggestions (
    id VARCHAR(36) PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    sender_role VARCHAR(50) NOT NULL,
    sender_name VARCHAR(150),
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    recipient_role VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(150),
    school_id VARCHAR(50),
    department_id VARCHAR(50),
    master_programme_id VARCHAR(50),
    programme_name VARCHAR(255),
    programme_batch_id VARCHAR(50),
    batch_name VARCHAR(255),
    programme_batch_course_id VARCHAR(50),
    course_name VARCHAR(255),
    context_type VARCHAR(50) NOT NULL,
    category VARCHAR(50) DEFAULT 'GENERAL',
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DELIVERED',
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_suggestions_recipient ON suggestions(recipient_id, read_at);
CREATE INDEX IF NOT EXISTS idx_suggestions_sender ON suggestions(sender_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_suggestions_batch ON suggestions(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_suggestions_course ON suggestions(programme_batch_course_id);

-- 4. Workflow Notifications (Authoritative Approvals)
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    source_entity_type VARCHAR(50) NOT NULL,
    source_entity_id VARCHAR(100) NOT NULL,
    deep_link VARCHAR(255),
    idempotency_key VARCHAR(190) UNIQUE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_id, read_at);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications(created_at DESC);
