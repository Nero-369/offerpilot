CREATE TABLE agent_runs (
  id UUID PRIMARY KEY,
  question TEXT NOT NULL,
  offer_id UUID REFERENCES offers(id) ON DELETE SET NULL,
  status VARCHAR(32) NOT NULL,
  answer TEXT,
  total_ms BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ
);

CREATE TABLE agent_skill_calls (
  id UUID PRIMARY KEY,
  run_id UUID NOT NULL REFERENCES agent_runs(id) ON DELETE CASCADE,
  skill_name VARCHAR(80) NOT NULL,
  input_summary TEXT NOT NULL,
  output_summary TEXT,
  status VARCHAR(32) NOT NULL,
  duration_ms BIGINT NOT NULL,
  cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_runs_created_at ON agent_runs(created_at DESC);
CREATE INDEX idx_agent_skill_calls_run_id ON agent_skill_calls(run_id);
