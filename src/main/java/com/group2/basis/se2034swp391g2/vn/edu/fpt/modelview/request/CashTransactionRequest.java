package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class CashTransactionRequest {

    // Loại phiếu: ALL, INCOME hoặc EXPENSE.
    private String type = "ALL";

    // Nhóm giao dịch: ALL hoặc một giá trị trong CashTransactionCategory.
    private String category = "ALL";

    // Nguồn phát sinh: ALL hoặc một giá trị trong CashTransactionSourceType.
    private String sourceType = "ALL";

    // Ngày bắt đầu để lọc giao dịch.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    // Ngày kết thúc để lọc giao dịch.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    // Từ khóa tìm theo mã chứng từ hoặc mô tả.
    private String keyword;

    // Trang hiện tại, bắt đầu từ 0 giống màn promotion.
    private int page = 0;

    // Số dòng trên một trang.
    private int size = 10;
}
