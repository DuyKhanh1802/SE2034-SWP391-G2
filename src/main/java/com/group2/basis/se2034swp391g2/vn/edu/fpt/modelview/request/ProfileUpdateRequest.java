package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.Gender;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.IdentityType;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^[0-9+\\-\\s]{8,20}$",
            message = "Số điện thoại phải từ 8 đến 20 ký tự và chỉ chứa số, dấu +, dấu - hoặc khoảng trắng"
    )
    private String phone;

    private String email;

    private Gender gender;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

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