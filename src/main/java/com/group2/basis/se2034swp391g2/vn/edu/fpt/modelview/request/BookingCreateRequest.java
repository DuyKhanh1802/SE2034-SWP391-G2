package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.Gender;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class BookingCreateRequest {

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String email;

    private Gender gender;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkInDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOutDate;

    private Integer adults;

    private Integer children;

    private List<Long> roomIds;

    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateOfBirth;

    private Long countryId;

    private String identityNumber;
    
    private String notes;

    
    private String action;

    private Boolean depositPaid;

    private BigDecimal depositAmount;

    private PaymentMethod depositMethod;

    private Map<Long, Integer> extraBedCounts;

    private List<Long> serviceIds;
    private Map<Long, Integer> serviceQuantities;
    private List<Integer> childAges;
}