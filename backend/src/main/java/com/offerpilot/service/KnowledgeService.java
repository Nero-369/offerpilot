package com.offerpilot.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeService {
    private static final int CANDIDATE_LIMIT = 20;
    private static final double RRF_K = 60.0;
    private final JdbcTemplate jdbc;
    private final TextChunker chunker;
    private final QwenEmbeddingClient embeddings;

    public KnowledgeService(JdbcTemplate jdbc, TextChunker chunker, QwenEmbeddingClient embeddings) {
        this.jdbc = jdbc;
        this.chunker = chunker;
        this.embeddings = embeddings;
    }

    @Transactional
    public DocumentSummary ingest(String title, String city, String sourceUrl, String content) {
        return ingest(title, city, sourceUrl, content, null, null, null, null, 1);
    }

    @Transactional
    public DocumentSummary ingest(String title, String city, String sourceUrl, String content,
                                  String policyType, LocalDate effectiveDate, LocalDate expiryDate,
                                  String versionLabel, Integer authorityLevel) {
        if (effectiveDate != null && expiryDate != null && expiryDate.isBefore(effectiveDate)) {
            throw new IllegalArgumentException("失效日期不能早于生效日期");
        }
        List<TextChunker.ParentChunk> parents = chunker.splitHierarchical(content);
        List<TextChunker.Chunk> children = parents.stream().flatMap(parent -> parent.children().stream()).toList();
        if (children.isEmpty()) throw new IllegalArgumentException("知识正文不能为空");
        int authority = Math.max(0, Math.min(authorityLevel == null ? 1 : authorityLevel, 3));
        UUID documentId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO knowledge_documents(id,title,city,source_url,content_length,chunk_count,status,created_at,
                  policy_type,effective_date,expiry_date,version_label,authority_level)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, documentId, title, blankToNull(city), sourceUrl, content.length(), children.size(), "EMBEDDING",
                Timestamp.from(now), blankToNull(policyType), effectiveDate, expiryDate, blankToNull(versionLabel), authority);

        Map<Integer, UUID> parentIds = new LinkedHashMap<>();
        for (TextChunker.ParentChunk parent : parents) {
            UUID parentId = UUID.randomUUID();
            parentIds.put(parent.index(), parentId);
            jdbc.update("""
                    INSERT INTO knowledge_parent_chunks(id,document_id,parent_index,section_title,content,token_estimate)
                    VALUES (?,?,?,?,?,?)
                    """, parentId, documentId, parent.index(), parent.sectionTitle(), parent.content(), parent.tokenEstimate());
        }

        for (int parentIndex = 0; parentIndex < parents.size(); parentIndex++) {
            TextChunker.ParentChunk parent = parents.get(parentIndex);
            List<TextChunker.Chunk> parentChildren = parent.children();
            for (int offset = 0; offset < parentChildren.size(); offset += 10) {
                List<TextChunker.Chunk> batch = parentChildren.subList(offset, Math.min(parentChildren.size(), offset + 10));
                List<float[]> vectors = embeddings.embed(batch.stream().map(TextChunker.Chunk::content).toList());
                for (int index = 0; index < batch.size(); index++) {
                    TextChunker.Chunk child = batch.get(index);
                    jdbc.update("""
                            INSERT INTO knowledge_chunks(id,document_id,chunk_index,content,token_estimate,embedding,parent_id,section_title)
                            VALUES (?,?,?,?,?,CAST(? AS vector),?,?)
                            """, UUID.randomUUID(), documentId, child.index(), child.content(), child.tokenEstimate(),
                            toVector(vectors.get(index)), parentIds.get(parentIndex), parent.sectionTitle());
                }
            }
        }
        jdbc.update("UPDATE knowledge_documents SET status='READY' WHERE id=?", documentId);
        return new DocumentSummary(documentId, title, blankToNull(city), sourceUrl, content.length(), children.size(),
                parents.size(), "READY", blankToNull(policyType), effectiveDate, expiryDate,
                blankToNull(versionLabel), authority, now);
    }

    public List<DocumentSummary> list() {
        return jdbc.query("""
                SELECT d.id,d.title,d.city,d.source_url,d.content_length,d.chunk_count,d.status,d.created_at,
                  d.policy_type,d.effective_date,d.expiry_date,d.version_label,d.authority_level,
                  (SELECT count(*) FROM knowledge_parent_chunks p WHERE p.document_id=d.id) parent_count
                FROM knowledge_documents d ORDER BY d.created_at DESC
                """, (rs, row) -> new DocumentSummary(rs.getObject("id", UUID.class), rs.getString("title"),
                rs.getString("city"), rs.getString("source_url"), rs.getInt("content_length"),
                rs.getInt("chunk_count"), rs.getInt("parent_count"), rs.getString("status"),
                rs.getString("policy_type"), localDate(rs, "effective_date"), localDate(rs, "expiry_date"),
                rs.getString("version_label"), rs.getInt("authority_level"), rs.getTimestamp("created_at").toInstant()));
    }

    public SearchResult search(String query, String city, int limit) {
        return search(query, new SearchFilters(city, null, LocalDate.now()), limit);
    }

    public SearchResult search(String query, SearchFilters filters, int requestedLimit) {
        long started = System.nanoTime();
        int limit = Math.max(1, Math.min(requestedLimit, 8));
        int candidates = Math.max(CANDIDATE_LIMIT, limit * 4);
        SearchFilters normalized = filters == null
                ? new SearchFilters(null, null, LocalDate.now())
                : new SearchFilters(blankToNull(filters.city()), blankToNull(filters.policyType()),
                    filters.asOf() == null ? LocalDate.now() : filters.asOf());

        float[] queryVector = embeddings.embedOne(query);
        long embedded = System.nanoTime();
        List<BaseHit> vectorHits = vectorSearch(toVector(queryVector), normalized, candidates);
        long vectorDone = System.nanoTime();
        List<BaseHit> keywordHits = keywordSearch(query, normalized, candidates);
        long keywordDone = System.nanoTime();
        List<RetrievedChunk> fused = fuse(vectorHits, keywordHits, limit);
        long completed = System.nanoTime();
        return new SearchResult(fused, elapsed(started, embedded), elapsed(embedded, vectorDone),
                elapsed(vectorDone, keywordDone), elapsed(keywordDone, completed), elapsed(started, completed),
                vectorHits.size(), keywordHits.size(), embeddings.model(), "pgvector+postgres-fts+trigram/RRF");
    }

    private List<BaseHit> vectorSearch(String vector, SearchFilters filters, int limit) {
        String sql = """
                SELECT c.id,c.parent_id,COALESCE(p.content,c.content) content,c.chunk_index,c.section_title,
                  d.id document_id,d.title,d.city,d.source_url,d.policy_type,d.effective_date,d.expiry_date,
                  d.version_label,d.authority_level,1-(c.embedding <=> CAST(? AS vector)) score
                FROM knowledge_chunks c
                JOIN knowledge_documents d ON d.id=c.document_id
                LEFT JOIN knowledge_parent_chunks p ON p.id=c.parent_id
                WHERE d.status='READY'
                  AND (? IS NULL OR d.city IS NULL OR d.city=?)
                  AND (? IS NULL OR d.policy_type IS NULL OR d.policy_type=?)
                  AND (d.effective_date IS NULL OR d.effective_date<=?)
                  AND (d.expiry_date IS NULL OR d.expiry_date>=?)
                ORDER BY c.embedding <=> CAST(? AS vector) LIMIT ?
                """;
        Date asOf = Date.valueOf(filters.asOf());
        return jdbc.query(sql, (rs, row) -> hit(rs), vector, filters.city(), filters.city(),
                filters.policyType(), filters.policyType(), asOf, asOf, vector, limit);
    }

    private List<BaseHit> keywordSearch(String query, SearchFilters filters, int limit) {
        String sql = """
                WITH q AS (SELECT websearch_to_tsquery('simple', ?) query)
                SELECT c.id,c.parent_id,COALESCE(p.content,c.content) content,c.chunk_index,c.section_title,
                  d.id document_id,d.title,d.city,d.source_url,d.policy_type,d.effective_date,d.expiry_date,
                  d.version_label,d.authority_level,
                  GREATEST(ts_rank_cd(c.search_vector,q.query),word_similarity(lower(?),lower(c.content))) score
                FROM knowledge_chunks c
                JOIN knowledge_documents d ON d.id=c.document_id
                LEFT JOIN knowledge_parent_chunks p ON p.id=c.parent_id CROSS JOIN q
                WHERE d.status='READY'
                  AND (? IS NULL OR d.city IS NULL OR d.city=?)
                  AND (? IS NULL OR d.policy_type IS NULL OR d.policy_type=?)
                  AND (d.effective_date IS NULL OR d.effective_date<=?)
                  AND (d.expiry_date IS NULL OR d.expiry_date>=?)
                  AND (c.search_vector@@q.query OR word_similarity(lower(?),lower(c.content))>0.08)
                ORDER BY score DESC LIMIT ?
                """;
        Date asOf = Date.valueOf(filters.asOf());
        return jdbc.query(sql, (rs, row) -> hit(rs), query, query, filters.city(), filters.city(),
                filters.policyType(), filters.policyType(), asOf, asOf, query, limit);
    }

    private List<RetrievedChunk> fuse(List<BaseHit> vectorHits, List<BaseHit> keywordHits, int limit) {
        Map<UUID, FusedHit> combined = new LinkedHashMap<>();
        for (int rank = 0; rank < vectorHits.size(); rank++) {
            BaseHit hit = vectorHits.get(rank);
            FusedHit fused = combined.computeIfAbsent(hit.id(), ignored -> new FusedHit(hit));
            fused.vectorRank = rank + 1;
            fused.vectorScore = hit.score();
            fused.rrf += 1.0 / (RRF_K + rank + 1) + hit.authorityLevel() * 0.0001;
        }
        for (int rank = 0; rank < keywordHits.size(); rank++) {
            BaseHit hit = keywordHits.get(rank);
            FusedHit fused = combined.computeIfAbsent(hit.id(), ignored -> new FusedHit(hit));
            fused.keywordRank = rank + 1;
            fused.keywordScore = hit.score();
            fused.rrf += 1.0 / (RRF_K + rank + 1) + hit.authorityLevel() * 0.0001;
        }
        Map<UUID, FusedHit> parentDeduplicated = new LinkedHashMap<>();
        combined.values().stream().sorted(Comparator.comparingDouble((FusedHit hit) -> hit.rrf).reversed())
                .forEach(hit -> {
                    UUID key = hit.base.parentId() == null ? hit.base.id() : hit.base.parentId();
                    parentDeduplicated.putIfAbsent(key, hit);
                });
        return parentDeduplicated.values().stream().limit(limit).map(FusedHit::result).toList();
    }

    private BaseHit hit(ResultSet rs) throws SQLException {
        return new BaseHit(rs.getObject("id", UUID.class), rs.getObject("parent_id", UUID.class),
                rs.getObject("document_id", UUID.class), rs.getString("title"), rs.getString("city"),
                rs.getString("source_url"), rs.getInt("chunk_index"), rs.getString("section_title"),
                rs.getString("content"), round(rs.getDouble("score"), 4), rs.getString("policy_type"),
                localDate(rs, "effective_date"), localDate(rs, "expiry_date"), rs.getString("version_label"),
                rs.getInt("authority_level"));
    }

    @Transactional
    public void delete(UUID id) { jdbc.update("DELETE FROM knowledge_documents WHERE id=?", id); }

    public boolean embeddingConfigured() { return embeddings.configured(); }
    public boolean sourceExists(String sourceUrl) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM knowledge_documents WHERE source_url=?)", Boolean.class, sourceUrl));
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private LocalDate localDate(ResultSet rs, String column) throws SQLException { Date value = rs.getDate(column); return value == null ? null : value.toLocalDate(); }
    private long elapsed(long from, long to) { return (to - from) / 1_000_000; }
    private double round(double value, int places) { double scale = Math.pow(10, places); return Math.round(value * scale) / scale; }
    private String toVector(float[] values) { StringBuilder value = new StringBuilder("["); for (int index = 0; index < values.length; index++) { if (index > 0) value.append(','); value.append(values[index]); } return value.append(']').toString(); }

    private record BaseHit(UUID id, UUID parentId, UUID documentId, String title, String city, String sourceUrl,
                           int chunkIndex, String sectionTitle, String content, double score, String policyType,
                           LocalDate effectiveDate, LocalDate expiryDate, String versionLabel, int authorityLevel) {}

    private final class FusedHit {
        final BaseHit base;
        int vectorRank;
        int keywordRank;
        double vectorScore;
        double keywordScore;
        double rrf;
        FusedHit(BaseHit base) { this.base = base; }
        RetrievedChunk result() {
            String mode = vectorRank > 0 && keywordRank > 0 ? "HYBRID" : vectorRank > 0 ? "VECTOR" : "KEYWORD";
            return new RetrievedChunk(base.id(), base.parentId(), base.documentId(), base.title(), base.city(),
                    base.sourceUrl(), base.chunkIndex(), base.sectionTitle(), base.content(), vectorScore,
                    keywordScore, round(rrf, 6), vectorRank, keywordRank, mode, base.policyType(),
                    base.effectiveDate(), base.expiryDate(), base.versionLabel(), base.authorityLevel());
        }
    }

    public record SearchFilters(String city, String policyType, LocalDate asOf) {}
    public record DocumentSummary(UUID id, String title, String city, String sourceUrl, int contentLength,
                                  int chunkCount, int parentCount, String status, String policyType,
                                  LocalDate effectiveDate, LocalDate expiryDate, String versionLabel,
                                  int authorityLevel, Instant createdAt) {}
    public record RetrievedChunk(UUID id, UUID parentId, UUID documentId, String title, String city,
                                 String sourceUrl, int chunkIndex, String sectionTitle, String content,
                                 double similarity, double keywordScore, double rrfScore, int vectorRank,
                                 int keywordRank, String retrievalMode, String policyType,
                                 LocalDate effectiveDate, LocalDate expiryDate, String versionLabel,
                                 int authorityLevel) {}
    public record SearchResult(List<RetrievedChunk> chunks, long embeddingMs, long vectorSearchMs,
                               long keywordSearchMs, long fusionMs, long totalMs, int vectorCandidates,
                               int keywordCandidates, String embeddingModel, String strategy) {}
}
