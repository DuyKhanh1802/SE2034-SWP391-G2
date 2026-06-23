package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingConfirmRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingConfirmView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OnlineBookingService {
    private static final BigDecimal SERVICE_CHARGE_RATE = BigDecimal.valueOf(0.05);
    private static final BigDecimal VAT_RATE = BigDecimal.valueOf(0.08);

    private static final int MAX_FIRST_NAME_LENGTH = 50;
    private static final int MAX_lIST_NAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 150;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_SPECIAL_REQUEST_LENGTH = 500;
    private static final int MAX_BOOKINGS_NIGHTS = 30;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9()+\\s-]{8,20}$");

    /*
     * TODO: đổi thông tin này theo tài khoản thật của khách sạn.
     */
    private static final String BANK_NAME = "Vietcombank";
    private static final String BANK_ACCOUNT_NUMBER = "0123456789";
    private static final String BANK_ACCOUNT_NAME = "VHOTEL HANOI";

    private final BookingSelectionService bookingSelectionService;
    private final PromotionService promotionService;
    private final MailService mailService;

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final RoomTypeVariantRepository roomTypeVariantRepository;
    private final ServiceRepository serviceRepository;
    private final PromotionRepository promotionRepository;

//    @Transactional(readOnly = true)
//    public BookingConfirmView prepareConfirmView(BookingConfirmRequest request){
//
//    }

    private void validateBasicBookingRequest(BookingConfirmRequest request){
        if(request == null){
            throw new IllegalArgumentException("Thông tin đặt phòng không được để trống");
        }

        if(request.getVariantIds() == null || request.getVariantIds().trim().isEmpty()){
            //throw new
        }

    }
}
