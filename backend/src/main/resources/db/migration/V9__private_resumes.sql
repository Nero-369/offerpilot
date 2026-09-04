CREATE TABLE user_resumes (
 user_id UUID PRIMARY KEY REFERENCES app_users(id),
 filename VARCHAR(500) NOT NULL,
 content TEXT NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
