package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class CashTransactionResponse {

    private Long id;

    private String documentCode;

    private Instant createdAt;

    private String type;

    private String typeDisplayName;

    private String categoryDisplayName;

    private BigDecimal amount;

    private String sourceDisplayName;

    private String statusDisplayName;
}
