package com.offerpilot.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_tasks")
public class AnalysisTask {
    public enum Status { QUEUED, CALCULATING, RETRIEVING, REASONING, COMPLETED, FAILED }
    @Id private UUID id;
    @Column(nullable = false) private UUID offerId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(nullable = false) private int progress;
    @Column(columnDefinition = "text") private String resultJson;
    @Column(columnDefinition = "text") private String errorMessage;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected AnalysisTask() {}
    public AnalysisTask(UUID offerId) { this.id=UUID.randomUUID(); this.offerId=offerId; this.status=Status.QUEUED; this.progress=0; this.createdAt=Instant.now(); this.updatedAt=createdAt; }
    public void advance(Status status, int progress){this.status=status;this.progress=progress;this.updatedAt=Instant.now();}
    public void complete(String resultJson){this.resultJson=resultJson;advance(Status.COMPLETED,100);}
    public void fail(String message){this.errorMessage=message;advance(Status.FAILED,100);}
    public UUID getId(){return id;} public UUID getOfferId(){return offerId;} public Status getStatus(){return status;} public int getProgress(){return progress;}
    public String getResultJson(){return resultJson;} public String getErrorMessage(){return errorMessage;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
