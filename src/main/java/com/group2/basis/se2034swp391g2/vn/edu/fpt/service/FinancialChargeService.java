package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FinancialChargeService {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private static final BigDecimal SERVICE_CHARGE_RATE = BigDecimal.valueOf(5);
    private static final BigDecimal VAT_RATE = BigDecimal.valueOf(8);
    private static final BigDecimal INVENTORY_VAT_RATE = BigDecimal.valueOf(8);
    private static final boolean TAX_ON_SERVICE_CHARGE = true;

    public BigDecimal getServiceChargeRate() {
        return SERVICE_CHARGE_RATE;
    }

    public BigDecimal getVatRate() {
        return VAT_RATE;
    }

    public BigDecimal getInventoryVatRate() {
        return INVENTORY_VAT_RATE;
    }

    public boolean isTaxOnServiceCharge() {
        return TAX_ON_SERVICE_CHARGE;
    }

    public BigDecimal toDecimalRate(BigDecimal ratePercent) {
        if (ratePercent == null) {
            return BigDecimal.ZERO;
        }
        return ratePercent.divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateRateAmount(BigDecimal baseAmount, BigDecimal rate) {
        if (baseAmount == null || rate == null) {
            return BigDecimal.ZERO;
        }
        return baseAmount.multiply(rate).divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP);
    }
}
