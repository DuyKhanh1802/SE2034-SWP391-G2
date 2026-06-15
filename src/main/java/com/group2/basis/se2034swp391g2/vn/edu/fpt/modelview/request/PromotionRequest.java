package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PromotionRequest {

    // Tên chiến dịch là thông tin bắt buộc và chỉ giới hạn tối đa 200 ký tự.
    @NotBlank(message = "Tên chiến dịch không được để trống.")
    @Size(max = 200, message = "Tên chiến dịch không được vượt quá 200 ký tự.")
    private String name;

    // Mô tả ngắn là thông tin bắt buộc và chỉ giới hạn tối đa 300 ký tự.
    @NotBlank(message = "Mô tả ngắn không được để trống.")
    @Size(max = 300, message = "Mô tả không được vượt quá 300 ký tự.")
    private String description;

    // Số tiền giảm phải có giá trị và phải lớn hơn 0.
    @NotNull(message = "Số tiền giảm không được để trống.")
    @Positive(message = "Số tiền giảm phải lớn hơn 0.")
    private BigDecimal discountAmount;

    // Giới hạn lượt dùng phải có giá trị và phải lớn hơn 0.
    @NotNull(message = "Giới hạn lượt dùng không được để trống.")
    @Positive(message = "Giới hạn lượt dùng phải lớn hơn 0.")
    private Integer usageLimit;

    private Boolean showOnHomepage = false;

    private Boolean featured = false;

    // URL ảnh khuyến mãi sau khi upload là thông tin bắt buộc trước khi lưu.
    @NotBlank(message = "Ảnh khuyến mãi không được để trống.")
    private String imageUrl;

    // Public ID của ảnh trên Cloudinary cũng cần có để quản lý ảnh đã upload.
    @NotBlank(message = "Ảnh khuyến mãi không được để trống.")
    private String imagePublicId;

    // Ngày bắt đầu là thông tin bắt buộc trên form.
    @NotBlank(message = "Ngày bắt đầu không được để trống.")
    private String validFromInput;

    // Ngày kết thúc là thông tin bắt buộc trên form.
    @NotBlank(message = "Ngày kết thúc không được để trống.")
    private String validToInput;
}
