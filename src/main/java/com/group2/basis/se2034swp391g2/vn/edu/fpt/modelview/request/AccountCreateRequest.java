package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import lombok.Data;

@Data
public class AccountCreateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private String roleName;
    private Long currentAdminId; // ID của Admin đang thực hiện tạo tài khoản (Phục vụ BR-27)
}