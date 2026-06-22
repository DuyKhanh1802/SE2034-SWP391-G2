package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionApplyResponse {
    private boolean valid;

    private String message;

    private Long promotionId;

    private String code;

    private String name;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;
}
