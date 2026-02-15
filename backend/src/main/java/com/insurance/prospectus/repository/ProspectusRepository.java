package com.insurance.prospectus.repository;

import com.insurance.common.entity.Prospectus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProspectusRepository extends JpaRepository<Prospectus, Long> {

    @Query("SELECT p FROM Prospectus p WHERE p.lead.id = :leadId ORDER BY p.createdAt DESC")
    List<Prospectus> findByLeadId(@Param("leadId") Long leadId);

    @Query("SELECT p FROM Prospectus p WHERE p.lead.id = :leadId ORDER BY p.version DESC")
    List<Prospectus> findByLeadIdOrderByVersionDesc(@Param("leadId") Long leadId);

    @Query("SELECT p FROM Prospectus p WHERE p.lead.id = :leadId AND p.version = :version")
    Optional<Prospectus> findByLeadIdAndVersion(
        @Param("leadId") Long leadId,
        @Param("version") Integer version
    );
}
