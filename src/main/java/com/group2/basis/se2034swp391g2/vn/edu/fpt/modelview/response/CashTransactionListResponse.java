package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashTransactionListResponse {

    // Danh sách giao dịch của trang hiện tại.
    private List<CashTransactionResponse> transactions;

    // Tổng số giao dịch sau khi lọc.
    private long totalTransactions;

    // Thông tin phân trang để template vẽ nút Trước/Sau.
    private int currentPage;

    private int totalPages;

    private int pageSize;

    private boolean hasPrevious;

    private boolean hasNext;
}
