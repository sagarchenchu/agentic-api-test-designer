package com.agentic.api.repository;

import com.agentic.api.entity.AgentRunHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentRunHistoryRepository extends JpaRepository<AgentRunHistoryEntity, String> {

    List<AgentRunHistoryEntity> findAllByOrderByCreatedAtDesc();
}
