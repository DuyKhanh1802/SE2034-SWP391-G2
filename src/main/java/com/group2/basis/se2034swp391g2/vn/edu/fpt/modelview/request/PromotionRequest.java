package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PromotionRequest {

    private String name;

    private String description;

    private BigDecimal discountAmount;

    private Integer usageLimit;

    private Boolean showOnHomepage = false;

    private Boolean featured = false;

    /*
     * URL ảnh khuyến mãi đã upload.
     */
    private String imageUrl;

    /*
     * Public ID của ảnh trên Cloudinary.
     */
    private String imagePublicId;

    private String validFromInput;

    private String validToInput;
}