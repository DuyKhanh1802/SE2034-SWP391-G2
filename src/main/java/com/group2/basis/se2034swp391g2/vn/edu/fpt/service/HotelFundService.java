package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
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
    public HotelFundSetting configureOpeningBalance(BigDecimal openingCashBalance,
                                                    BigDecimal openingTransferBalance,
                                                    BigDecimal openingCardBalance,
                                                    User configuredBy) {
        BigDecimal cash = normalizeNonNegative(openingCashBalance);
        BigDecimal transfer = normalizeNonNegative(openingTransferBalance);
        BigDecimal card = normalizeNonNegative(openingCardBalance);
        BigDecimal total = cash.add(transfer).add(card);

        HotelFundSetting setting = HotelFundSetting.builder()
                .openingBalance(total)
                .openingCashBalance(cash)
                .openingTransferBalance(transfer)
                .openingCardBalance(card)
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
    public BigDecimal getOpeningBalanceByFundMethod(PaymentMethod fundMethod) {
        HotelFundSetting setting = getCurrentSetting();
        if (setting == null) {
            return BigDecimal.ZERO;
        }
        return switch (fundMethod) {
            case CASH -> nullToZero(setting.getOpeningCashBalance());
            case TRANSFER -> nullToZero(setting.getOpeningTransferBalance());
            case CARD -> nullToZero(setting.getOpeningCardBalance());
        };
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalance() {
        return getOpeningBalance()
                .add(cashTransactionService.getTotalIncome())
                .subtract(cashTransactionService.getTotalExpense());
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalanceByFundMethod(PaymentMethod fundMethod) {
        return getOpeningBalanceByFundMethod(fundMethod)
                .add(cashTransactionService.getTotalIncomeByFundMethod(fundMethod))
                .subtract(cashTransactionService.getTotalExpenseByFundMethod(fundMethod));
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

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
