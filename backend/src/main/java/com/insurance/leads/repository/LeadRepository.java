package com.insurance.leads.repository;

import com.insurance.common.entity.Lead;
import com.insurance.common.entity.Lead.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    /**
     * Find all leads assigned to a specific agent
     */
    List<Lead> findByAssignedAgentId(Long agentId);

    /**
     * Find all leads assigned to a specific agent with pagination
     */
    Page<Lead> findByAssignedAgentId(Long agentId, Pageable pageable);

    /**
     * Find all leads by status
     */
    List<Lead> findByStatus(LeadStatus status);

    /**
     * Find all leads by status with pagination
     */
    Page<Lead> findByStatus(LeadStatus status, Pageable pageable);

    /**
     * Find leads by assigned agent and status with pagination
     */
    Page<Lead> findByAssignedAgentIdAndStatus(Long agentId, LeadStatus status, Pageable pageable);

    /**
     * Search leads by name, phone, or email
     */
    @Query("SELECT l FROM Lead l WHERE " +
           "LOWER(l.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Lead> searchLeads(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Search leads by assigned agent and search term
     */
    @Query("SELECT l FROM Lead l WHERE l.assignedAgent.id = :agentId AND (" +
           "LOWER(l.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Lead> searchLeadsByAgent(@Param("agentId") Long agentId,
                                   @Param("searchTerm") String searchTerm,
                                   Pageable pageable);

    /**
     * Find leads by status with search term
     */
    @Query("SELECT l FROM Lead l WHERE l.status = :status AND (" +
           "LOWER(l.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Lead> searchLeadsByStatus(@Param("status") LeadStatus status,
                                    @Param("searchTerm") String searchTerm,
                                    Pageable pageable);

    /**
     * Find leads by assigned agent, status, and search term
     */
    @Query("SELECT l FROM Lead l WHERE l.assignedAgent.id = :agentId AND l.status = :status AND (" +
           "LOWER(l.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Lead> searchLeadsByAgentAndStatus(@Param("agentId") Long agentId,
                                            @Param("status") LeadStatus status,
                                            @Param("searchTerm") String searchTerm,
                                            Pageable pageable);

    /**
     * Count leads by status
     */
    Long countByStatus(LeadStatus status);

    /**
     * Count leads by assigned agent and status
     */
    Long countByAssignedAgentIdAndStatus(Long agentId, LeadStatus status);
}
