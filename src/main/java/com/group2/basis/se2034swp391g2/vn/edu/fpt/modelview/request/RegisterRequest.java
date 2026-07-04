package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "First name không được để trống")
    @Size(min = 2, max = 50, message = "First name phải từ 2 đến 50 ký tự")
    @Pattern(
            regexp = "^[\\p{L}]+(?:[\\s'-][\\p{L}]+)*$",
            message = "First name chỉ được chứa chữ cái, khoảng trắng, dấu nháy hoặc dấu gạch nối"
    )
    private String firstName;

    @NotBlank(message = "Last name không được để trống")
    @Size(min = 2, max = 50, message = "Last name phải từ 2 đến 50 ký tự")
    @Pattern(
            regexp = "^[\\p{L}]+(?:[\\s'-][\\p{L}]+)*$",
            message = "Last name chỉ được chứa chữ cái, khoảng trắng, dấu nháy hoặc dấu gạch nối"
    )
    private String lastName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    @Pattern(
            regexp = "(?i)^[a-z0-9._%+-]+@gmail\\.com$",
            message = "Email phải có định dạng ...@gmail.com"
    )
    private String email;

    @NotNull(message = "Vui lòng chọn quốc gia")
    private Long countryId;

    private String phoneCode;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^[0-9]{8,15}$",
            message = "Số điện thoại phải gồm 8 đến 15 chữ số"
    )
    private String phone;

    @NotBlank(message = "Password không được để trống")
    @Size(max = 72, message = "Password không được vượt quá 72 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{8,}$",
            message = "Password phải có ít nhất 8 ký tự, 1 chữ hoa, 1 chữ thường và 1 số"
    )
    private String passwordHash;

    @NotBlank(message = "Confirm password không được để trống")
    private String confirmPassword;
}