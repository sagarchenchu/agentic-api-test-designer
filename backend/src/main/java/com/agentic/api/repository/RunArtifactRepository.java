package com.agentic.api.repository;

import com.agentic.api.entity.RunArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunArtifactRepository extends JpaRepository<RunArtifactEntity, Long> {

    List<RunArtifactEntity> findByRunIdOrderByCreatedAtAsc(String runId);

    void deleteByRunId(String runId);
}
