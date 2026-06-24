package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.FinancialChargeSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinancialChargeSettingRepository extends JpaRepository<FinancialChargeSetting, Long> {
    @Query("""
            SELECT setting
            FROM FinancialChargeSetting setting
            WHERE setting.isActive = true
            AND setting.effectiveFrom <= :date
            AND (setting.effectiveTo IS NULL OR setting.effectiveTo >= :date)
            ORDER BY setting.effectiveFrom DESC
            """)
    List<FinancialChargeSetting> findCurrentSettings(@Param("date") LocalDate date);

    Optional<FinancialChargeSetting> findTopByIsActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(LocalDate date);

    Optional<FinancialChargeSetting> findTopByIsActiveTrueOrderByEffectiveFromDesc();

    @Query("""
        SELECT f
        FROM FinancialChargeSetting f
        WHERE f.isActive = true
          AND f.effectiveFrom <= :today
          AND (f.effectiveTo IS NULL OR f.effectiveTo >= :today)
        ORDER BY f.effectiveFrom DESC
    """)
    Optional<FinancialChargeSetting> findCurrentSetting(@Param("today") LocalDate today);
}
