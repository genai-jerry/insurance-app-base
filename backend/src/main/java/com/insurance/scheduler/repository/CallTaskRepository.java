package com.insurance.scheduler.repository;

import com.insurance.common.entity.CallTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CallTaskRepository extends JpaRepository<CallTask, Long> {

    @Query("SELECT ct FROM CallTask ct WHERE ct.agent.id = :agentId ORDER BY ct.scheduledTime ASC")
    List<CallTask> findByAgentId(@Param("agentId") Long agentId);

    @Query("SELECT ct FROM CallTask ct WHERE ct.lead.id = :leadId ORDER BY ct.scheduledTime DESC")
    List<CallTask> findByLeadId(@Param("leadId") Long leadId);

    @Query("SELECT ct FROM CallTask ct WHERE ct.scheduledTime BETWEEN :startTime AND :endTime ORDER BY ct.scheduledTime ASC")
    List<CallTask> findByScheduledTimeBetween(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT ct FROM CallTask ct WHERE ct.status = :status ORDER BY ct.scheduledTime ASC")
    List<CallTask> findByStatus(@Param("status") CallTask.TaskStatus status);

    @Query("SELECT ct FROM CallTask ct WHERE ct.agent.id = :agentId AND ct.scheduledTime BETWEEN :startTime AND :endTime ORDER BY ct.scheduledTime ASC")
    List<CallTask> findByAgentIdAndScheduledTimeBetween(
        @Param("agentId") Long agentId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
