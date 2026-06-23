package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class CashTransactionResponse {

    // Dữ liệu một dòng giao dịch dùng để hiển thị trong bảng list.
    private Long id;

    private String documentCode;

    private Instant createdAt;

    private String type;

    private String typeDisplayName;

    private String categoryDisplayName;

    private String category;

    private BigDecimal amount;

    private String sourceDisplayName;

    private String sourceType;

    private String statusDisplayName;
}
