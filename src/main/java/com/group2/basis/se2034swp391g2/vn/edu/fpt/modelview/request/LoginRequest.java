package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class LoginRequest {

    @NotBlank(message = "Username không được để trống")
    private String email;

    @NotBlank(message = "Password không dc để trống")
    private String password;
}
