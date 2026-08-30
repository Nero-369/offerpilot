CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE offers (
  id UUID PRIMARY KEY,
  company VARCHAR(255) NOT NULL,
  role VARCHAR(255) NOT NULL,
  city VARCHAR(120) NOT NULL,
  monthly_salary NUMERIC(12,2) NOT NULL CHECK (monthly_salary > 0),
  salary_months NUMERIC(5,2) NOT NULL CHECK (salary_months >= 12),
  annual_bonus NUMERIC(12,2) NOT NULL DEFAULT 0,
  annual_housing_cost NUMERIC(12,2) NOT NULL DEFAULT 0,
  job_description VARCHAR(8000),
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE analysis_tasks (
  id UUID PRIMARY KEY,
  offer_id UUID NOT NULL REFERENCES offers(id) ON DELETE CASCADE,
  status VARCHAR(32) NOT NULL,
  progress INTEGER NOT NULL CHECK (progress BETWEEN 0 AND 100),
  result_json TEXT,
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_analysis_tasks_offer_id ON analysis_tasks(offer_id);
CREATE INDEX idx_analysis_tasks_status ON analysis_tasks(status);

CREATE TABLE policy_documents (
  id UUID PRIMARY KEY,
  city VARCHAR(120) NOT NULL,
  policy_type VARCHAR(120) NOT NULL,
  title VARCHAR(500) NOT NULL,
  source_url TEXT NOT NULL,
  effective_date DATE,
  expiry_date DATE,
  content TEXT NOT NULL,
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_policy_city_type ON policy_documents(city, policy_type);
