package com.offerpilot.api;

import com.offerpilot.service.KnowledgeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledge;
    public KnowledgeController(KnowledgeService knowledge){this.knowledge=knowledge;}
    @GetMapping public List<KnowledgeService.DocumentSummary> list(){return knowledge.list();}
    @GetMapping("/status") public Status status(){return new Status(knowledge.embeddingConfigured(),knowledge.list().size(),knowledge.list().stream().mapToInt(KnowledgeService.DocumentSummary::chunkCount).sum(),"pgvector/HNSW","overlap-900/140");}
    @PostMapping public KnowledgeService.DocumentSummary ingest(@Valid @RequestBody IngestRequest request){return knowledge.ingest(request.title(),request.city(),request.sourceUrl(),request.content());}
    @PostMapping("/search") public KnowledgeService.SearchResult search(@Valid @RequestBody SearchRequest request){return knowledge.search(request.query(),request.city(),request.limit()==null?5:request.limit());}
    @DeleteMapping("/{id}") public void delete(@PathVariable UUID id){knowledge.delete(id);}
    public record IngestRequest(@NotBlank @Size(max=500) String title,@Size(max=120) String city,@NotBlank @Size(max=2000) String sourceUrl,@NotBlank @Size(max=100000) String content){}
    public record SearchRequest(@NotBlank @Size(max=2000) String query,@Size(max=120) String city,@Min(1) @Max(8) Integer limit){}
    public record Status(boolean embeddingConfigured,int documents,int chunks,String index,String chunkStrategy){}
}
