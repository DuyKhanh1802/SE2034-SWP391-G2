package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CashTransactionCreateRequest {

    // Loại phiếu manager chọn trên form: phiếu thu hoặc phiếu chi.
    private CashTransactionType type = CashTransactionType.INCOME;

    // Số tiền manager nhập, luôn nhập số dương.
    private BigDecimal amount;

    // Nội dung giải thích vì sao lập phiếu.
    private String description;
}
