package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.time.LocalDate;
import java.util.List;

public class RevenueReportResponse {
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final RevenueReportSummaryResponse summary;
    private final List<RevenueReportRowResponse> rows;
    private final String errorMessage;

    public RevenueReportResponse(LocalDate fromDate,
                                 LocalDate toDate,
                                 RevenueReportSummaryResponse summary,
                                 List<RevenueReportRowResponse> rows,
                                 String errorMessage) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.summary = summary;
        this.rows = rows == null ? List.of() : rows;
        this.errorMessage = errorMessage;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public RevenueReportSummaryResponse getSummary() {
        return summary;
    }

    public List<RevenueReportRowResponse> getRows() {
        return rows;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
