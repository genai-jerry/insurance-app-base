package com.insurance.leads.repository;

import com.insurance.common.entity.LeadActivity;
import com.insurance.common.entity.LeadActivity.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {

    /**
     * Find all activities for a specific lead ordered by creation date (newest first)
     */
    List<LeadActivity> findByLeadIdOrderByCreatedAtDesc(Long leadId);

    /**
     * Find all activities for a specific lead ordered by creation date (oldest first)
     */
    List<LeadActivity> findByLeadIdOrderByCreatedAtAsc(Long leadId);

    /**
     * Find all activities by lead and type
     */
    List<LeadActivity> findByLeadIdAndType(Long leadId, ActivityType type);

    /**
     * Count activities by lead
     */
    Long countByLeadId(Long leadId);

    /**
     * Count activities by lead and type
     */
    Long countByLeadIdAndType(Long leadId, ActivityType type);
}
