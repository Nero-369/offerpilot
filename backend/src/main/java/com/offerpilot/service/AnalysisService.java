package com.offerpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.api.DecisionReport;
import com.offerpilot.domain.AnalysisTask;
import com.offerpilot.domain.Offer;
import com.offerpilot.repository.AnalysisTaskRepository;
import com.offerpilot.repository.OfferRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisService {
    private final OfferRepository offers; private final AnalysisTaskRepository tasks; private final IncomeCalculator calculator;
    private final DecisionEngine decisionEngine; private final ObjectMapper objectMapper; private final StringRedisTemplate redis;
    private final TaskExecutor taskExecutor;
    public AnalysisService(OfferRepository offers, AnalysisTaskRepository tasks, IncomeCalculator calculator, DecisionEngine decisionEngine, ObjectMapper objectMapper, StringRedisTemplate redis, TaskExecutor taskExecutor) {
        this.offers=offers;this.tasks=tasks;this.calculator=calculator;this.decisionEngine=decisionEngine;this.objectMapper=objectMapper;this.redis=redis;this.taskExecutor=taskExecutor;
    }
    public AnalysisTask create(UUID offerId) {
        offers.findById(offerId).orElseThrow(() -> new IllegalArgumentException("Offer not found"));
        AnalysisTask task=tasks.save(new AnalysisTask(offerId)); taskExecutor.execute(() -> runAsync(task.getId())); return task;
    }
    public void runAsync(UUID taskId) {
        try {
            AnalysisTask task=tasks.findById(taskId).orElseThrow(); Offer offer=offers.findById(task.getOfferId()).orElseThrow();
            update(task, AnalysisTask.Status.CALCULATING, 25); IncomeCalculator.Result income=calculator.calculate(offer);
            update(task, AnalysisTask.Status.RETRIEVING, 50);
            update(task, AnalysisTask.Status.REASONING, 75); DecisionEngine.AiAssessment ai=decisionEngine.assess(offer);
            DecisionReport report=new DecisionReport(income.grossAnnualIncome(), income.taxAndSocialInsurance(), income.disposableAnnualIncome(), ai.jobMatchScore(), ai.growthScore(), ai.stabilityScore(), 90, ai.recommendation(), ai.strengths(), ai.risks(), List.of(new DecisionReport.Evidence("Offer薪资结构", "用户输入", "当前", "HIGH"), new DecisionReport.Evidence("薪税估算", "确定性Java计算引擎", "2026", "MEDIUM")));
            task.complete(objectMapper.writeValueAsString(report)); tasks.save(task); cache(task);
        } catch (Exception ex) { tasks.findById(taskId).ifPresent(task -> {task.fail(ex.getMessage());tasks.save(task);cache(task);}); }
    }
    private void update(AnalysisTask task, AnalysisTask.Status status, int progress){task.advance(status,progress);tasks.save(task);cache(task);}
    private void cache(AnalysisTask task){try{redis.opsForValue().set("offerpilot:task:"+task.getId(), task.getStatus()+":"+task.getProgress(), Duration.ofHours(2));}catch(RuntimeException ignored){}}
    public AnalysisTask get(UUID id){return tasks.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found"));}
}
