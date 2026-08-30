CREATE TABLE knowledge_documents (
  id UUID PRIMARY KEY,
  title VARCHAR(500) NOT NULL,
  city VARCHAR(120),
  source_url TEXT NOT NULL,
  content_length INTEGER NOT NULL,
  chunk_count INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE knowledge_chunks (
  id UUID PRIMARY KEY,
  document_id UUID NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
  chunk_index INTEGER NOT NULL,
  content TEXT NOT NULL,
  token_estimate INTEGER NOT NULL,
  embedding vector(1024) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(document_id, chunk_index)
);

CREATE INDEX idx_knowledge_document_city ON knowledge_documents(city);
CREATE INDEX idx_knowledge_chunk_document ON knowledge_chunks(document_id);
CREATE INDEX idx_knowledge_chunk_embedding_hnsw
  ON knowledge_chunks USING hnsw (embedding vector_cosine_ops);
