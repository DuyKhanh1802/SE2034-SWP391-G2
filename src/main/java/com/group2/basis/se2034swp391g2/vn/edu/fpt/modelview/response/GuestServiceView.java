package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ServiceCategoryType;
import lombok.*;

@Data
public class GuestServiceView {
    private Long serviceId;
    private String name;
    private String description;
    private BigDecimal price;

    private String categoryName;
    private ServiceCategoryType categoryType;

    private String imageUrl;

}
