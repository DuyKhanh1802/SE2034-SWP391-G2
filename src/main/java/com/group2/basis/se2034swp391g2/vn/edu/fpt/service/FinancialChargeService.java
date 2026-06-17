package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.FinancialChargeSetting;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.FinancialChargeSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FinancialChargeService {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final FinancialChargeSettingRepository financialChargeSettingRepository;

    @Transactional(readOnly = true)
    public FinancialChargeSetting getCurrentSetting() {
        LocalDate today = LocalDate.now();
        return financialChargeSettingRepository
                .findTopByIsActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(today)
                .or(() -> financialChargeSettingRepository.findTopByIsActiveTrueOrderByEffectiveFromDesc())
                .orElseGet(this::defaultSetting);
    }

    public BigDecimal calculateRateAmount(BigDecimal baseAmount, BigDecimal rate) {
        if (baseAmount == null || rate == null) {
            return BigDecimal.ZERO;
        }
        return baseAmount.multiply(rate).divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP);
    }

    private FinancialChargeSetting defaultSetting() {
        return FinancialChargeSetting.builder()
                .serviceChargeRate(BigDecimal.valueOf(5))
                .vatRate(BigDecimal.valueOf(8))
                .inventoryVatRate(BigDecimal.valueOf(8))
                .taxOnServiceCharge(true)
                .effectiveFrom(LocalDate.now())
                .isActive(true)
                .build();
    }
}
