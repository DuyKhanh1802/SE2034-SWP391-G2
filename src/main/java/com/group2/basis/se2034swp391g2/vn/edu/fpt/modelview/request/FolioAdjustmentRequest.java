package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FolioAdjustmentRequest {
    private Long bookingDetailId;
    private String description;
    private BigDecimal amount;
    private String adjustmentReason;
}
