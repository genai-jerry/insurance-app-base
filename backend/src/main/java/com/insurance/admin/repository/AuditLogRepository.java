package com.insurance.admin.repository;

import com.insurance.common.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT al FROM AuditLog al WHERE al.actor.id = :actorId ORDER BY al.createdAt DESC")
    List<AuditLog> findByActorId(@Param("actorId") Long actorId);

    @Query("SELECT al FROM AuditLog al WHERE al.actor.id = :actorId ORDER BY al.createdAt DESC")
    Page<AuditLog> findByActorId(@Param("actorId") Long actorId, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.entity = :entity ORDER BY al.createdAt DESC")
    List<AuditLog> findByEntity(@Param("entity") String entity);

    @Query("SELECT al FROM AuditLog al WHERE al.entity = :entity ORDER BY al.createdAt DESC")
    Page<AuditLog> findByEntity(@Param("entity") String entity, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.entity = :entity AND al.entityId = :entityId ORDER BY al.createdAt DESC")
    List<AuditLog> findByEntityAndEntityId(
        @Param("entity") String entity,
        @Param("entityId") Long entityId
    );

    @Query("SELECT al FROM AuditLog al ORDER BY al.createdAt DESC")
    Page<AuditLog> findAllOrderByCreatedAtDesc(Pageable pageable);
}
