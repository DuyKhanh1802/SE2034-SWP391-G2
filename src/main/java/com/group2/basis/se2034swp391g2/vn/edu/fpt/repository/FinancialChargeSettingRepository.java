package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.FinancialChargeSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface FinancialChargeSettingRepository extends JpaRepository<FinancialChargeSetting, Long> {
    Optional<FinancialChargeSetting> findTopByIsActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(LocalDate date);

    Optional<FinancialChargeSetting> findTopByIsActiveTrueOrderByEffectiveFromDesc();
}
