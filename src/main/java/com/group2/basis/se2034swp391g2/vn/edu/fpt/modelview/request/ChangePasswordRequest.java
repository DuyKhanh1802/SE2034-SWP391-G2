package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required.")
    private String currentPassword;

    @NotBlank(message = "New password is required.")
    @Size(min = 6, max = 50, message = "New password must be from 6 to 50 characters.")
    private String newPassword;

    @NotBlank(message = "Confirm password is required.")
    private String confirmPassword;
}
