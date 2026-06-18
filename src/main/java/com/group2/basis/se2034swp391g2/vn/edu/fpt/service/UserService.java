package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    Page<User> getAccountPage(String keyword,
                              RoleName role,
                              String activeStatus,
                              ApprovalStatus approvalStatus,
                              Pageable pageable);
    User getUserById(Long id);
    void updateUser(Long id, AccountUpdateRequest request);
    String resetPassword(Long id);
}
