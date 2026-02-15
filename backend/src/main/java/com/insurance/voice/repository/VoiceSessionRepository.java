package com.insurance.voice.repository;

import com.insurance.common.entity.VoiceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoiceSessionRepository extends JpaRepository<VoiceSession, Long> {

    @Query("SELECT vs FROM VoiceSession vs WHERE vs.lead.id = :leadId ORDER BY vs.startedAt DESC")
    List<VoiceSession> findByLeadId(@Param("leadId") Long leadId);

    @Query("SELECT vs FROM VoiceSession vs WHERE vs.agent.id = :agentId ORDER BY vs.startedAt DESC")
    List<VoiceSession> findByAgentId(@Param("agentId") Long agentId);

    @Query("SELECT vs FROM VoiceSession vs WHERE vs.status = :status ORDER BY vs.startedAt DESC")
    List<VoiceSession> findByStatus(@Param("status") VoiceSession.SessionStatus status);

    Optional<VoiceSession> findBySessionId(String sessionId);
}
