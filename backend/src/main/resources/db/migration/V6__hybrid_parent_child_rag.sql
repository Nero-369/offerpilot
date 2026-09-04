CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE knowledge_documents
  ADD COLUMN policy_type VARCHAR(120),
  ADD COLUMN effective_date DATE,
  ADD COLUMN expiry_date DATE,
  ADD COLUMN version_label VARCHAR(120),
  ADD COLUMN authority_level SMALLINT NOT NULL DEFAULT 1 CHECK (authority_level BETWEEN 0 AND 3),
  ADD COLUMN metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE INDEX idx_knowledge_document_policy_dates ON knowledge_documents(policy_type, effective_date, expiry_date);

CREATE TABLE knowledge_parent_chunks (
  id UUID PRIMARY KEY,
  document_id UUID NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
  parent_index INTEGER NOT NULL,
  section_title VARCHAR(500),
  content TEXT NOT NULL,
  token_estimate INTEGER NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(document_id, parent_index)
);

ALTER TABLE knowledge_chunks
  ADD COLUMN parent_id UUID REFERENCES knowledge_parent_chunks(id) ON DELETE CASCADE,
  ADD COLUMN section_title VARCHAR(500),
  ADD COLUMN search_vector TSVECTOR GENERATED ALWAYS AS (
    to_tsvector('simple', COALESCE(section_title, '') || ' ' || content)
  ) STORED;

CREATE INDEX idx_knowledge_parent_document ON knowledge_parent_chunks(document_id);
CREATE INDEX idx_knowledge_chunk_parent ON knowledge_chunks(parent_id);
CREATE INDEX idx_knowledge_chunk_search_vector ON knowledge_chunks USING gin(search_vector);
CREATE INDEX idx_knowledge_chunk_content_trgm ON knowledge_chunks USING gin(content gin_trgm_ops);

CREATE TABLE rag_evaluation_runs (
  id UUID PRIMARY KEY,
  dataset_name VARCHAR(200) NOT NULL,
  case_count INTEGER NOT NULL,
  hit_at_k DOUBLE PRECISION NOT NULL,
  mean_reciprocal_rank DOUBLE PRECISION NOT NULL,
  keyword_coverage DOUBLE PRECISION NOT NULL,
  average_latency_ms DOUBLE PRECISION NOT NULL,
  details JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
