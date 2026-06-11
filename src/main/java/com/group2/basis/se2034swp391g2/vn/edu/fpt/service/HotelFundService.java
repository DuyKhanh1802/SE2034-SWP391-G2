package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.HotelFundSetting;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.HotelFundSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
    public HotelFundSetting configureOpeningBalance(BigDecimal openingBalance, User configuredBy) {
        if (openingBalance == null || openingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Vốn đầu kỳ không được âm.");
        }

        HotelFundSetting setting = HotelFundSetting.builder()
                .openingBalance(openingBalance)
                .configuredBy(configuredBy)
                .build();

        return hotelFundSettingRepository.save(setting);
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
}
