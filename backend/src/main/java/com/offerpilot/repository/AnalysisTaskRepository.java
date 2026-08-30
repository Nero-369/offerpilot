package com.offerpilot.repository;
import com.offerpilot.domain.AnalysisTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, UUID> {
    Optional<AnalysisTask> findFirstByOfferIdOrderByCreatedAtDesc(UUID offerId);
    List<AnalysisTask> findTop50ByOrderByCreatedAtDesc();
}
