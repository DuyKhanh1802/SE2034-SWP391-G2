package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class OccupancyReportRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private Long variantId;

    private String sortBy = "variantNameAsc";

    private Integer page = 0;

    public int getSafePage() {
        if (page == null || page < 0) {
            return 0;
        }

        return page;
    }
}
