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
        return financialChargeSettingRepository
                .findCurrentSettings(LocalDate.now())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy cấu hình phí và thuế đang có hiệu lực."));
    }

    public BigDecimal calculateRateAmount(BigDecimal baseAmount, BigDecimal rate) {
        if (baseAmount == null || rate == null) {
            return BigDecimal.ZERO;
        }
        return baseAmount.multiply(rate).divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP);
    }
}
