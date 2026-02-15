package com.insurance.admin.repository;

import com.insurance.common.entity.AdminSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminSettingRepository extends JpaRepository<AdminSetting, Long> {

    @Query("SELECT a FROM AdminSetting a WHERE a.key = :key")
    Optional<AdminSetting> findByKey(@Param("key") String key);

    boolean existsByKey(String key);
}
