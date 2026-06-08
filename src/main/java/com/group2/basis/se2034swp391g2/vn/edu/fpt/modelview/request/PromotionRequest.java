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
     * Ảnh khuyến mãi đã được upload lên Cloudinary trước.
     * Form chỉ gửi lại URL ảnh để lưu vào promotion.
     */
    private String imageUrl;

    /*
     * Public ID của ảnh trên Cloudinary.
     * Dùng để update/delete ảnh sau này nếu cần.
     */
    private String imagePublicId;

    private String validFromInput;

    private String validToInput;
}