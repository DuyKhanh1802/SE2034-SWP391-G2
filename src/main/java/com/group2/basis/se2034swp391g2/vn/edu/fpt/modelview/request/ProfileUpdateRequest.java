package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.Gender;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.IdentityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class ProfileUpdateRequest {

    @NotBlank(message = "Tên không được để trống")
    @Size(min = 2, max = 50, message = "Tên phải từ 2 đến 50 ký tự")
    @Pattern(
            regexp = "^[\\p{L} ]+$",
            message = "Tên chỉ được chứa chữ cái và khoảng trắng"
    )
    private String firstName;

    @NotBlank(message = "Họ không được để trống")
    @Size(min = 2, max = 50, message = "Họ phải từ 2 đến 50 ký tự")
    @Pattern(
            regexp = "^[\\p{L} ]+$",
            message = "Họ chỉ được chứa chữ cái và khoảng trắng"
    )
    private String lastName;


    private String phoneCode;


    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự")
    private String phone;

    private String email;

    @NotNull(message = "Vui lòng chọn giới tính")
    private Gender gender;

    @NotNull(message = "Năm sinh không được để trống")
    private Integer birthYear;

    private Long countryId;

    private String countryName;

    private String countryCode;

    private IdentityType identityType;

    @Size(max = 50, message = "Số CCCD/Hộ chiếu không được vượt quá 50 ký tự")
    private String identityNumber;

    private String avatarUrl;

    private MultipartFile avatarFile;

    private boolean guest;
}