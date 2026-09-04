ALTER TABLE rag_evaluation_runs
  ADD COLUMN faithfulness DOUBLE PRECISION,
  ADD COLUMN answer_relevance DOUBLE PRECISION,
  ADD COLUMN context_precision DOUBLE PRECISION,
  ADD COLUMN context_recall DOUBLE PRECISION,
  ADD COLUMN answer_correctness DOUBLE PRECISION,
  ADD COLUMN judge_model VARCHAR(120);
