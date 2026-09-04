CREATE TABLE chat_conversations (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL DEFAULT 'anonymous',
    offer_id UUID NULL REFERENCES offers(id) ON DELETE SET NULL,
    title VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user','assistant','system')),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX chat_messages_conversation_created_idx
    ON chat_messages(conversation_id, created_at DESC);

