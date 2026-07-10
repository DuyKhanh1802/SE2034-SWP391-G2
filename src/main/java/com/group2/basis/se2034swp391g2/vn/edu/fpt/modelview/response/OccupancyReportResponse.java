package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.time.LocalDate;
import java.util.List;

public class OccupancyReportResponse {
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final long reportDays;
    private final OccupancyReportSummaryResponse summary;
    private final List<OccupancyReportRowResponse> rows;
    private final int currentPage;
    private final int pageSize;
    private final int totalItems;
    private final int totalPages;
    private final int startItem;
    private final int endItem;
    private final String errorMessage;

    public OccupancyReportResponse(LocalDate fromDate,
                                   LocalDate toDate,
                                   long reportDays,
                                   OccupancyReportSummaryResponse summary,
                                   List<OccupancyReportRowResponse> rows,
                                   int currentPage,
                                   int pageSize,
                                   int totalItems,
                                   int totalPages,
                                   int startItem,
                                   int endItem,
                                   String errorMessage) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.reportDays = reportDays;
        this.summary = summary;
        this.rows = rows == null ? List.of() : rows;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
        this.startItem = startItem;
        this.endItem = endItem;
        this.errorMessage = errorMessage;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public long getReportDays() {
        return reportDays;
    }

    public OccupancyReportSummaryResponse getSummary() {
        return summary;
    }

    public List<OccupancyReportRowResponse> getRows() {
        return rows;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getStartItem() {
        return startItem;
    }

    public int getEndItem() {
        return endItem;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
