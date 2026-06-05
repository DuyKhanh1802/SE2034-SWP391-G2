package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Role;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.Nationalized;

@Data
public class RegisterRequest {
    @Nationalized
    @NotBlank(message = "FirstName không được để trống")
    @Size(min = 2,max = 50,message = "Firstname phải từ 2-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z_]+$",message = "FirstName chỉ được chứa chữ")
    private String firstName;

    @Nationalized
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

    private Long countryId;

    private String phoneCode;


}
