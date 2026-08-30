package com.offerpilot.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class KnowledgeService {
    private final JdbcTemplate jdbc;
    private final TextChunker chunker;
    private final QwenEmbeddingClient embeddings;
    public KnowledgeService(JdbcTemplate jdbc, TextChunker chunker, QwenEmbeddingClient embeddings) {
        this.jdbc=jdbc; this.chunker=chunker; this.embeddings=embeddings;
    }

    @Transactional
    public DocumentSummary ingest(String title, String city, String sourceUrl, String content) {
        List<TextChunker.Chunk> chunks=chunker.split(content);
        if(chunks.isEmpty()) throw new IllegalArgumentException("知识正文不能为空");
        UUID documentId=UUID.randomUUID(); Instant now=Instant.now();
        jdbc.update("INSERT INTO knowledge_documents(id,title,city,source_url,content_length,chunk_count,status,created_at) VALUES (?,?,?,?,?,?,?,?)",
            documentId,title,blankToNull(city),sourceUrl,content.length(),chunks.size(),"EMBEDDING",Timestamp.from(now));
        for(int offset=0;offset<chunks.size();offset+=10){
            List<TextChunker.Chunk> batch=chunks.subList(offset,Math.min(chunks.size(),offset+10));
            List<float[]> vectors=embeddings.embed(batch.stream().map(TextChunker.Chunk::content).toList());
            for(int i=0;i<batch.size();i++){
                TextChunker.Chunk chunk=batch.get(i);
                jdbc.update("INSERT INTO knowledge_chunks(id,document_id,chunk_index,content,token_estimate,embedding) VALUES (?,?,?,?,?,CAST(? AS vector))",
                    UUID.randomUUID(),documentId,chunk.index(),chunk.content(),chunk.tokenEstimate(),toVector(vectors.get(i)));
            }
        }
        jdbc.update("UPDATE knowledge_documents SET status='READY' WHERE id=?",documentId);
        return new DocumentSummary(documentId,title,blankToNull(city),sourceUrl,content.length(),chunks.size(),"READY",now);
    }

    public List<DocumentSummary> list() {
        return jdbc.query("SELECT id,title,city,source_url,content_length,chunk_count,status,created_at FROM knowledge_documents ORDER BY created_at DESC",
            (rs,n)->new DocumentSummary(rs.getObject("id",UUID.class),rs.getString("title"),rs.getString("city"),rs.getString("source_url"),rs.getInt("content_length"),rs.getInt("chunk_count"),rs.getString("status"),rs.getTimestamp("created_at").toInstant()));
    }

    public SearchResult search(String query, String city, int limit) {
        long start=System.nanoTime(); float[] vector=embeddings.embedOne(query); long embedded=System.nanoTime();
        String sql="""
          SELECT c.id,c.content,c.chunk_index,d.id document_id,d.title,d.city,d.source_url,
                 1-(c.embedding <=> CAST(? AS vector)) similarity
          FROM knowledge_chunks c JOIN knowledge_documents d ON d.id=c.document_id
          WHERE d.status='READY' AND (? IS NULL OR d.city IS NULL OR d.city=?)
          ORDER BY c.embedding <=> CAST(? AS vector) LIMIT ?
          """;
        String v=toVector(vector); String normalized=blankToNull(city);
        List<RetrievedChunk> hits=jdbc.query(sql,(rs,n)->new RetrievedChunk(rs.getObject("id",UUID.class),rs.getObject("document_id",UUID.class),rs.getString("title"),rs.getString("city"),rs.getString("source_url"),rs.getInt("chunk_index"),rs.getString("content"),Math.round(rs.getDouble("similarity")*10000d)/10000d),v,normalized,normalized,v,Math.max(1,Math.min(limit,8)));
        long done=System.nanoTime();
        return new SearchResult(hits,(embedded-start)/1_000_000,(done-embedded)/1_000_000,embeddings.model());
    }

    @Transactional public void delete(UUID id){jdbc.update("DELETE FROM knowledge_documents WHERE id=?",id);}
    public boolean embeddingConfigured(){return embeddings.configured();}
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
    private String toVector(float[] values){StringBuilder b=new StringBuilder("[");for(int i=0;i<values.length;i++){if(i>0)b.append(',');b.append(values[i]);}return b.append(']').toString();}

    public record DocumentSummary(UUID id,String title,String city,String sourceUrl,int contentLength,int chunkCount,String status,Instant createdAt){}
    public record RetrievedChunk(UUID id,UUID documentId,String title,String city,String sourceUrl,int chunkIndex,String content,double similarity){}
    public record SearchResult(List<RetrievedChunk> chunks,long embeddingMs,long vectorSearchMs,String embeddingModel){}
}
