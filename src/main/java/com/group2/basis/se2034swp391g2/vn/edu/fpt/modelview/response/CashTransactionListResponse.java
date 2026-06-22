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

    private List<CashTransactionResponse> transactions;

    private long totalTransactions;

    private int currentPage;

    private int totalPages;

    private int pageSize;

    private boolean hasPrevious;

    private boolean hasNext;
}
