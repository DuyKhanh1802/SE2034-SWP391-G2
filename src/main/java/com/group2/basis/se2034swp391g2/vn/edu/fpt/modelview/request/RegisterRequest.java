package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "FirstName không được để trống")
    @Size(min = 2,max = 50,message = "Firstname phải từ 2-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z_]+$",message = "FirstName chỉ được chứa chữ")
    private String firstName;


    @NotBlank(message = "Lastname không được để trống")
    @Size(min = 2,max = 50,message = "Lastname phải từ 2-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z_]+$",message = "LastName chỉ được chữa chữ")
    private String lastName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]+$",message = "Số điện thoại phải chứa số")
    private String phone;

    @NotBlank(message = "Password không được để trống")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{8,}$",message = "Password phải có ít nhất 8 ký tự, 1 chữ hoa, 1 chữ thường, 1 số")
    private String passwordHash;

    @NotBlank(message = "Confirm password không được để trống")
    private String confirmPassword;

    @NotNull(message = "Vui lòng chọn quốc tịch")
    private Integer nationalityId; // Khách chọn từ Dropdown Quốc gia ở FE gửi về ID

    @NotBlank(message = "Số CCCD hoặc Hộ chiếu không được để trống")
    @Size(max = 20, message = "Mã số định danh không được vượt quá 20 ký tự")
    private String identityCard; // Dùng chung trường này cho cả CCCD và Passport
}
