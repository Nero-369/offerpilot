package com.offerpilot.api;

import com.offerpilot.service.KnowledgeService;
import com.offerpilot.service.RagEvaluationService;
import com.offerpilot.service.UrlKnowledgeImportService;
import com.offerpilot.service.UploadedDocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledge;
    private final RagEvaluationService evaluations;
    private final UrlKnowledgeImportService urlImporter;
    private final UploadedDocumentService uploadedDocuments;

    public KnowledgeController(KnowledgeService knowledge, RagEvaluationService evaluations,
                               UrlKnowledgeImportService urlImporter, UploadedDocumentService uploadedDocuments) {
        this.knowledge = knowledge;
        this.evaluations = evaluations;
        this.urlImporter = urlImporter;
        this.uploadedDocuments = uploadedDocuments;
    }

    @GetMapping
    public List<KnowledgeService.DocumentSummary> list() { return knowledge.list(); }

    @GetMapping("/status")
    public Status status() {
        List<KnowledgeService.DocumentSummary> documents = knowledge.list();
        return new Status(knowledge.embeddingConfigured(), documents.size(),
                documents.stream().mapToInt(KnowledgeService.DocumentSummary::chunkCount).sum(),
                documents.stream().mapToInt(KnowledgeService.DocumentSummary::parentCount).sum(),
                "pgvector/HNSW + PostgreSQL FTS/trigram", "parent-2600/child-650/overlap-100", "RRF(k=60)");
    }

    @PostMapping
    public KnowledgeService.DocumentSummary ingest(@Valid @RequestBody IngestRequest request) {
        return knowledge.ingest(request.title(), request.city(), request.sourceUrl(), request.content(),
                request.policyType(), request.effectiveDate(), request.expiryDate(), request.versionLabel(),
                request.authorityLevel());
    }

    @PostMapping("/search")
    public KnowledgeService.SearchResult search(@Valid @RequestBody SearchRequest request) {
        return knowledge.search(request.query(), new KnowledgeService.SearchFilters(request.city(),
                request.policyType(), request.asOf()), request.limit() == null ? 5 : request.limit());
    }

    @PostMapping("/url-preview")
    public List<UrlKnowledgeImportService.Preview> previewUrls(@Valid @RequestBody UrlPreviewRequest request) {
        return urlImporter.preview(request.urls());
    }

    @PostMapping(value = "/file-preview", consumes = "multipart/form-data")
    public UploadedDocumentService.ParsedDocument previewFile(@RequestPart("file") MultipartFile file) {
        return uploadedDocuments.parse(file);
    }

    @PostMapping("/evaluate")
    public RagEvaluationService.EvaluationRun evaluate(@Valid @RequestBody EvaluationRequest request) {
        return evaluations.evaluate(request.datasetName(), request.cases());
    }

    @GetMapping("/evaluations")
    public List<RagEvaluationService.EvaluationSummary> evaluationHistory() { return evaluations.history(); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) { knowledge.delete(id); }

    public record IngestRequest(@NotBlank @Size(max = 500) String title,
                                @Size(max = 120) String city,
                                @NotBlank @Size(max = 2000) String sourceUrl,
                                @NotBlank @Size(max = 100000) String content,
                                @Size(max = 120) String policyType,
                                LocalDate effectiveDate,
                                LocalDate expiryDate,
                                @Size(max = 120) String versionLabel,
                                @Min(0) @Max(3) Integer authorityLevel) {}

    public record SearchRequest(@NotBlank @Size(max = 2000) String query,
                                @Size(max = 120) String city,
                                @Size(max = 120) String policyType,
                                LocalDate asOf,
                                @Min(1) @Max(8) Integer limit) {}

    public record UrlPreviewRequest(@NotEmpty @Size(max = 20) List<String> urls) {}

    public record EvaluationRequest(@NotBlank @Size(max = 200) String datasetName,
                                    @NotEmpty @Size(max = 100) List<RagEvaluationService.EvaluationCase> cases) {}

    public record Status(boolean embeddingConfigured, int documents, int chunks, int parentChunks,
                         String index, String chunkStrategy, String fusion) {}
}
