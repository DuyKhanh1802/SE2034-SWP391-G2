package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class ProfileUpdateRequest {

    @NotBlank(message = "Tên đầu không được để trống")
    @Size(min = 2,max = 50, message = "Tên đầu phải phải lớn hơn 2 ký tự")
    @Pattern(regexp = "^[a-zA-Z ]+$",message = "Tên đầu phải chữa chữ cái không được chứa chữ cái đặc biệt")
    private String firstName;

    @NotBlank(message = "Tên cuối không được để trống")
    @Size(min = 2,max = 20,message = "Tên cuối phải từ 2 ký tự trở lên")
    @Pattern(regexp = "[a-zA_Z_]+$",message = "Tên cuối phải chứa các chữ cái")
    private String lastName;

    @NotBlank(message = "Số đện thoại không được để trống")
    @Pattern(regexp = "^[0-9+\\-\\s]{8,20}$",message = "Số điện thoại phải từ 8 chữ số trở lên")
    private String phoneNumber;

    private String email;

    private String gender;

    @Past(message = "Date of birth must be in the past.")
    private LocalDate dateofBirth;

    private Long countryId;

    private String passportNumber;

    private String identifyNumber;

    private MultipartFile avaterFile;

    private boolean guest;

}
