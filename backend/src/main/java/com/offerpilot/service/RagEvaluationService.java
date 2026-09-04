package com.offerpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RagEvaluationService {
    private final KnowledgeService knowledge;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public RagEvaluationService(KnowledgeService knowledge, JdbcTemplate jdbc, ObjectMapper json) {
        this.knowledge = knowledge;
        this.jdbc = jdbc;
        this.json = json;
    }

    public EvaluationRun evaluate(String datasetName, List<EvaluationCase> cases) {
        if (cases == null || cases.isEmpty()) throw new IllegalArgumentException("评测集不能为空");
        List<CaseResult> details = new ArrayList<>();
        double hits = 0, reciprocalRanks = 0, coverage = 0, latency = 0;
        for (EvaluationCase test : cases) {
            int k = Math.max(1, Math.min(test.k() == null ? 5 : test.k(), 8));
            KnowledgeService.SearchResult result = knowledge.search(test.query(),
                    new KnowledgeService.SearchFilters(test.city(), test.policyType(),
                            test.asOf() == null ? LocalDate.now() : test.asOf()), k);
            int firstExpectedRank = expectedRank(result.chunks(), test.expectedDocumentIds(), test.expectedSourceUrls());
            boolean hit = firstExpectedRank > 0;
            double caseCoverage = keywordCoverage(result.chunks(), test.expectedKeywords());
            if (hit) { hits++; reciprocalRanks += 1.0 / firstExpectedRank; }
            coverage += caseCoverage;
            latency += result.totalMs();
            details.add(new CaseResult(test.query(), hit, firstExpectedRank, caseCoverage, result.totalMs(),
                    result.chunks().stream().map(KnowledgeService.RetrievedChunk::documentId).distinct().toList()));
        }
        UUID id = UUID.randomUUID();
        int count = cases.size();
        EvaluationRun run = new EvaluationRun(id, datasetName == null || datasetName.isBlank() ? "default" : datasetName,
                count, round(hits / count), round(reciprocalRanks / count), round(coverage / count),
                Math.round(latency / count), details, Instant.now());
        try {
            jdbc.update("""
                    INSERT INTO rag_evaluation_runs(id,dataset_name,case_count,hit_at_k,mean_reciprocal_rank,
                      keyword_coverage,average_latency_ms,details) VALUES (?,?,?,?,?,?,?,CAST(? AS jsonb))
                    """, run.id(), run.datasetName(), run.caseCount(), run.hitAtK(), run.meanReciprocalRank(),
                    run.keywordCoverage(), run.averageLatencyMs(), json.writeValueAsString(run.details()));
        } catch (Exception exception) {
            throw new IllegalStateException("保存RAG评测结果失败", exception);
        }
        return run;
    }

    public List<EvaluationSummary> history() {
        return jdbc.query("""
                SELECT id,dataset_name,case_count,hit_at_k,mean_reciprocal_rank,keyword_coverage,
                  average_latency_ms,created_at FROM rag_evaluation_runs ORDER BY created_at DESC LIMIT 30
                """, (rs, row) -> new EvaluationSummary(rs.getObject("id", UUID.class), rs.getString("dataset_name"),
                rs.getInt("case_count"), rs.getDouble("hit_at_k"), rs.getDouble("mean_reciprocal_rank"),
                rs.getDouble("keyword_coverage"), rs.getLong("average_latency_ms"),
                rs.getTimestamp("created_at").toInstant()));
    }

    private int expectedRank(List<KnowledgeService.RetrievedChunk> chunks, List<UUID> expectedIds, List<String> expectedUrls) {
        if ((expectedIds == null || expectedIds.isEmpty()) && (expectedUrls == null || expectedUrls.isEmpty())) return 0;
        for (int index = 0; index < chunks.size(); index++) {
            KnowledgeService.RetrievedChunk hit = chunks.get(index);
            if (expectedIds != null && expectedIds.contains(hit.documentId())) return index + 1;
            if (expectedUrls != null && expectedUrls.contains(hit.sourceUrl())) return index + 1;
        }
        return 0;
    }

    private double keywordCoverage(List<KnowledgeService.RetrievedChunk> chunks, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return 1;
        String retrieved = chunks.stream().map(KnowledgeService.RetrievedChunk::content)
                .reduce("", (left, right) -> left + "\n" + right).toLowerCase(Locale.ROOT);
        long matched = keywords.stream().filter(keyword -> keyword != null &&
                retrieved.contains(keyword.toLowerCase(Locale.ROOT))).count();
        return (double) matched / keywords.size();
    }

    private double round(double value) { return Math.round(value * 10_000d) / 10_000d; }

    public record EvaluationCase(String query, String city, String policyType, LocalDate asOf,
                                 List<UUID> expectedDocumentIds, List<String> expectedSourceUrls,
                                 List<String> expectedKeywords, String groundTruth, Integer k) {}
    public record CaseResult(String query, boolean hit, int firstExpectedRank, double keywordCoverage,
                             long latencyMs, List<UUID> retrievedDocumentIds) {}
    public record EvaluationRun(UUID id, String datasetName, int caseCount, double hitAtK,
                                double meanReciprocalRank, double keywordCoverage, long averageLatencyMs,
                                List<CaseResult> details, Instant createdAt) {}
    public record EvaluationSummary(UUID id, String datasetName, int caseCount, double hitAtK,
                                    double meanReciprocalRank, double keywordCoverage,
                                    long averageLatencyMs, Instant createdAt) {}
}
