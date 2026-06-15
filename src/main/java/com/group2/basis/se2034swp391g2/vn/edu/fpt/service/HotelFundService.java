package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.HotelFundSetting;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.HotelFundSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class HotelFundService {
    private final HotelFundSettingRepository hotelFundSettingRepository;
    private final CashTransactionService cashTransactionService;

    @Transactional(readOnly = true)
    public HotelFundSetting getCurrentSetting() {
        return hotelFundSettingRepository.findTopByOrderByConfiguredAtDesc().orElse(null);
    }

    @Transactional
    public boolean addCapital(BigDecimal amount, User configuredBy) {
        BigDecimal capitalAmount = normalizeNonNegative(amount);
        HotelFundSetting setting = getCurrentSetting();

        if (setting == null) {
            HotelFundSetting newSetting = HotelFundSetting.builder()
                    .openingBalance(capitalAmount)
                    .configuredBy(configuredBy)
                    .build();
            hotelFundSettingRepository.save(newSetting);
            return true;
        }

        cashTransactionService.createCapitalInjection(
                capitalAmount,
                "Rot them von vao quy khach san",
                configuredBy
        );
        return false;
    }

    @Transactional(readOnly = true)
    public BigDecimal getOpeningBalance() {
        HotelFundSetting setting = getCurrentSetting();
        return setting == null ? BigDecimal.ZERO : setting.getOpeningBalance();
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalance() {
        return getOpeningBalance()
                .add(cashTransactionService.getTotalIncome())
                .subtract(cashTransactionService.getTotalExpense());
    }

    private BigDecimal normalizeNonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Von dau ky khong duoc am.");
        }
        return value.setScale(0, RoundingMode.HALF_UP);
    }

}
