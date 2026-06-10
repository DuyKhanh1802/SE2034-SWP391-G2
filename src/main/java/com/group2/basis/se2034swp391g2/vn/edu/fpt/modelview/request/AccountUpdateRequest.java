package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AccountUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    @NotEmpty(message = "Vui lòng chọn ít nhất một vai trò.")
    private List<Long> roleIds;
    private Boolean roleUpdateRequested;
    private Boolean isActive;

    // Cập nhật theo BR-23
    private ApprovalStatus approvalStatus;
    private String approvalNote;
    private Long currentAdminId; // ID của Admin đang thực hiện Edit/Review
}
