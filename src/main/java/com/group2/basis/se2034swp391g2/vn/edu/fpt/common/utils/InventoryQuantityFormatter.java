package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("inventoryQuantityFormatter")
public class InventoryQuantityFormatter {

    public String format(BigDecimal quantity) {
        if (quantity == null) {
            return "0";
        }
        if (quantity.stripTrailingZeros().scale() <= 0) {
            return quantity.setScale(0, RoundingMode.UNNECESSARY).toPlainString();
        }
        return quantity.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
