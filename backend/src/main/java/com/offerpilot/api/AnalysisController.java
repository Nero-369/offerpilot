package com.offerpilot.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.domain.AnalysisTask;
import com.offerpilot.service.AnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;
import com.offerpilot.repository.AnalysisTaskRepository;

@RestController
@RequestMapping("/api/v1/analyses")
public class AnalysisController {
    private final AnalysisService service; private final AnalysisTaskRepository tasks; private final ObjectMapper objectMapper;
    public AnalysisController(AnalysisService service,AnalysisTaskRepository tasks,ObjectMapper objectMapper){this.service=service;this.tasks=tasks;this.objectMapper=objectMapper;}
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED) public TaskResponse create(@RequestParam UUID offerId){return TaskResponse.from(service.create(offerId),objectMapper);}
    @GetMapping("/{id}") public TaskResponse get(@PathVariable UUID id){return TaskResponse.from(service.get(id),objectMapper);}
    @GetMapping("/latest") public TaskResponse latest(@RequestParam UUID offerId){return tasks.findFirstByOfferIdOrderByCreatedAtDesc(offerId).map(task -> TaskResponse.from(task,objectMapper)).orElse(null);}
    @GetMapping public List<TaskResponse> list(){return tasks.findTop50ByOrderByCreatedAtDesc().stream().map(task -> TaskResponse.from(task,objectMapper)).toList();}
    public record TaskResponse(UUID id, UUID offerId, String status, int progress, Object report, String error, java.time.Instant createdAt, java.time.Instant updatedAt) {
        static TaskResponse from(AnalysisTask task,ObjectMapper mapper){Object report=null;try{if(task.getResultJson()!=null)report=mapper.readValue(task.getResultJson(),Object.class);}catch(JsonProcessingException ignored){}return new TaskResponse(task.getId(),task.getOfferId(),task.getStatus().name(),task.getProgress(),report,task.getErrorMessage(),task.getCreatedAt(),task.getUpdatedAt());}
    }
}
