package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/*
 * Response cho màn danh sách khuyến mãi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionListResponse {

    private List<PromotionResponse> promotions;

    private long totalPromotions;

    private long activePromotions;

    private long scheduledPromotions;

    private long expiredPromotions;

    private long inactivePromotions;

    private int currentPage;

    private int totalPages;

    private int pageSize;

    private boolean hasPrevious;

    private boolean hasNext;
}