package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Data
public class GuestFolioTransactionView {
    private Long id;
    private Instant postedAt;
    private String description;
    private String category;
    private BigDecimal chargeAmount;
    private BigDecimal paymentAmount;
    private BigDecimal balance;
}
