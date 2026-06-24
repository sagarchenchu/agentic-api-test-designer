package com.agentic.api.repository;

import com.agentic.api.entity.ExternalOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExternalOperationRepository extends JpaRepository<ExternalOperationEntity, Long> {

    List<ExternalOperationEntity> findByRunIdOrderByCreatedAtAsc(String runId);

    void deleteByRunId(String runId);
}
