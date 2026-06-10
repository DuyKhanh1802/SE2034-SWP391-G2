package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PromotionListResponse {

    private List<PromotionResponse> promotions;

    private long totalPromotions;

    private long activePromotions;

    private long scheduledPromotions;

    private long expiredPromotions;

    private long inactivePromotions;
}