package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountUpdateRequest {

    @NotNull(message = "Vui lòng chọn một vai trò.")
    private Long roleId;

    private Boolean roleUpdateRequested;
    private Boolean isActive;

    // Cập nhật theo BR-23
    private ApprovalStatus approvalStatus;
    private String approvalNote;
    private Long currentAdminId; // ID của Admin đang thực hiện Edit/Review
}
