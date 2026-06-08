package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DiscountType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PromotionRequest {

    private String code;

    private String name;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal maxDiscount;

    private Integer usageLimit;

    private Boolean isActive = true;

    private String validFromInput;

    private String validToInput;
}