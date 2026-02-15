package com.insurance.email.repository;

import com.insurance.common.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    @Query("SELECT el FROM EmailLog el WHERE el.lead.id = :leadId ORDER BY el.createdAt DESC")
    List<EmailLog> findByLeadId(@Param("leadId") Long leadId);

    @Query("SELECT el FROM EmailLog el WHERE el.status = :status ORDER BY el.createdAt DESC")
    List<EmailLog> findByStatus(@Param("status") EmailLog.EmailStatus status);

    @Query("SELECT el FROM EmailLog el WHERE el.agent.id = :agentId ORDER BY el.createdAt DESC")
    List<EmailLog> findByAgentId(@Param("agentId") Long agentId);
}
