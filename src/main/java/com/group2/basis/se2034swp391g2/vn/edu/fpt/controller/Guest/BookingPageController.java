package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Guest;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingConfirmRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingCompleteResult;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingConfirmView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingSuccessView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionApplyResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.BookingServiceProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.BookingSelectionService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.MailService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.OnlineBookingService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PromotionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@RequestMapping("/page/booking")
public class BookingPageController {

    private final BookingSelectionService bookingSelectionService;
    private final PromotionService promotionService;
    private final OnlineBookingService onlineBookingService;
    private final MailService mailService;

    @GetMapping("/services")
    public String bookingService(
            @RequestParam String variantIds,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkInDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOutDate,

            @RequestParam(defaultValue = "1") Integer adults,
            @RequestParam(defaultValue = "0") Integer children,
            @RequestParam(defaultValue = "1") Integer roomCount,
            @RequestParam(required = false) String roomGuests,
            @RequestParam(required = false) String promoCode,

            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "recommended") String sort,
            @RequestParam(defaultValue = "ALL") String priceFilter,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,

            Model model
    ) {
        String selectedCategory = bookingSelectionService.normalizeCategory(category);
        String selectedSort = bookingSelectionService.normalizeSort(sort);
        String selectedPriceFilter = bookingSelectionService.normalizePriceFilter(priceFilter);

        List<GuestRoomVariantProjection> selectedRooms =
                bookingSelectionService.getSelectRoomsForService(
                        variantIds,
                        checkInDate,
                        checkOutDate
                );

        Page<BookingServiceProjection> servicePage =
                bookingSelectionService.getBookingService(
                        selectedCategory,
                        selectedSort,
                        selectedPriceFilter,
                        page,
                        size
                );

        long nights = 0;

        if (checkInDate != null && checkOutDate != null && checkOutDate.isAfter(checkInDate)) {
            nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        }

        BigDecimal roomSubtotal =
                bookingSelectionService.calculateRoomSubtotal(selectedRooms, nights);

        BigDecimal serviceCharge = roomSubtotal.multiply(BigDecimal.valueOf(0.05));
        BigDecimal vat = roomSubtotal.add(serviceCharge).multiply(BigDecimal.valueOf(0.08));

        PromotionApplyResponse promotionResult =
                promotionService.checkPromotionCode(promoCode);

        BigDecimal promotionDiscountAmount = BigDecimal.ZERO;

        if (promotionResult.isValid()) {
            promotionDiscountAmount = promotionResult.getDiscountAmount();
        }

        BigDecimal totalBeforeDiscount = roomSubtotal.add(serviceCharge).add(vat);

        if (promotionDiscountAmount.compareTo(totalBeforeDiscount) > 0) {
            promotionDiscountAmount = totalBeforeDiscount;
        }

        BigDecimal grandTotal = totalBeforeDiscount.subtract(promotionDiscountAmount);

        model.addAttribute("variantIds", variantIds);
        model.addAttribute("selectedRooms", selectedRooms);

        model.addAttribute("services", servicePage.getContent());
        model.addAttribute("servicePage", servicePage);

        model.addAttribute("checkInDate", checkInDate);
        model.addAttribute("checkOutDate", checkOutDate);
        model.addAttribute("nights", nights);

        model.addAttribute("adults", adults);
        model.addAttribute("children", children);
        model.addAttribute("roomCount", roomCount);
        model.addAttribute("roomGuests", roomGuests);
        model.addAttribute("promoCode", promoCode);

        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("selectedSort", selectedSort);
        model.addAttribute("selectedPriceFilter", selectedPriceFilter);

        model.addAttribute("promotionResult", promotionResult);
        model.addAttribute("promotionDiscountAmount", promotionDiscountAmount);
        model.addAttribute("promotionMessage", promotionResult.getMessage());
        model.addAttribute("promotionValid", promotionResult.isValid());

        model.addAttribute("roomSubtotal", roomSubtotal);
        model.addAttribute("serviceCharge", serviceCharge);
        model.addAttribute("vat", vat);
        model.addAttribute("grandTotal", grandTotal);

        return "guest/BookingServices";
    }

    @PostMapping("/confirm")
    public String bookingConfirm(
            @ModelAttribute BookingConfirmRequest request,
            Model model
    ) {
        return renderBookingConfirm(request, model);
    }

    @GetMapping("/confirm")
    public String bookingConfirmWithoutService(
            @RequestParam String variantIds,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkInDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOutDate,

            @RequestParam(defaultValue = "1") Integer adults,
            @RequestParam(defaultValue = "0") Integer children,
            @RequestParam(defaultValue = "1") Integer roomCount,
            @RequestParam(required = false) String roomGuests,
            @RequestParam(required = false) String promoCode,

            Model model
    ) {
        BookingConfirmRequest request = new BookingConfirmRequest();

        request.setVariantIds(variantIds);
        request.setCheckInDate(checkInDate);
        request.setCheckOutDate(checkOutDate);
        request.setAdults(adults);
        request.setChildren(children);
        request.setRoomCount(roomCount);
        request.setRoomGuests(roomGuests);
        request.setPromoCode(promoCode);

        return renderBookingConfirm(request, model);
    }

    @PostMapping("/complete")
    public String completeBooking(
            @ModelAttribute BookingConfirmRequest request,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        try {
            Boolean emailVerified =
                    (Boolean) session.getAttribute("BOOKING_EMAIL_VERIFIED");

            String verifiedEmail =
                    (String) session.getAttribute("BOOKING_EMAIL_VERIFIED_EMAIL");

            String requestEmail = request.getGuestEmail() != null
                    ? request.getGuestEmail().trim().toLowerCase()
                    : "";

            if (emailVerified == null
                    || !emailVerified
                    || verifiedEmail == null
                    || !verifiedEmail.equals(requestEmail)) {

                BookingConfirmView confirmView =
                        onlineBookingService.prepareConfirmView(request);

                model.addAttribute("request", request);
                model.addAttribute("confirmView", confirmView);
                model.addAttribute(
                        "errorMessage",
                        "Vui lòng xác thực email bằng mã OTP trước khi xác nhận đặt phòng."
                );

                return "guest/BookingConfirm";
            }

            BookingCompleteResult result =
                    onlineBookingService.completeOnlineBooking(request);

            session.removeAttribute("BOOKING_EMAIL_VERIFIED");
            session.removeAttribute("BOOKING_EMAIL_VERIFIED_EMAIL");

            redirectAttributes.addAttribute(
                    "bookingReference",
                    result.getBookingReference()
            );

            return "redirect:/page/booking/success";

        } catch (IllegalArgumentException e) {
            BookingConfirmView confirmView =
                    onlineBookingService.prepareConfirmView(request);

            model.addAttribute("request", request);
            model.addAttribute("confirmView", confirmView);
            model.addAttribute("errorMessage", e.getMessage());

            return "guest/BookingConfirm";
        }
    }

    @GetMapping("/success")
    public String bookingSuccess(
            @RequestParam String bookingReference,
            Model model
    ) {
        BookingSuccessView successView =
                onlineBookingService.getBookingSuccessView(bookingReference);

        model.addAttribute("successView", successView);

        return "guest/BookingSuccess";
    }

    @PostMapping("/send-email-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendBookingEmailOtp(
            @RequestParam String email,
            HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();

        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập email trước khi gửi mã OTP.");
            return ResponseEntity.badRequest().body(response);
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (!normalizedEmail.matches("^[A-Za-z0-9._%+-]+@gmail\\.com$")) {
            response.put("success", false);
            response.put("message", "Email phải đúng định dạng Gmail, ví dụ: example@gmail.com.");
            return ResponseEntity.badRequest().body(response);
        }

        String otp = generateBookingOtp();

        session.setAttribute("BOOKING_EMAIL_OTP", otp);
        session.setAttribute("BOOKING_EMAIL_OTP_EMAIL", normalizedEmail);
        session.setAttribute("BOOKING_EMAIL_OTP_EXPIRE_AT", Instant.now().plusSeconds(300));

        mailService.sendBookingEmailVerificationOtpEmail(normalizedEmail, otp);

        response.put("success", true);
        response.put("message", "Mã OTP đã được gửi về email của quý khách. Vui lòng kiểm tra hộp thư.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyBookingEmailOtp(
            @RequestParam String email,
            @RequestParam String otp,
            HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();

        String savedOtp =
                (String) session.getAttribute("BOOKING_EMAIL_OTP");

        String savedEmail =
                (String) session.getAttribute("BOOKING_EMAIL_OTP_EMAIL");

        Instant expireAt =
                (Instant) session.getAttribute("BOOKING_EMAIL_OTP_EXPIRE_AT");

        if (savedOtp == null || savedEmail == null || expireAt == null) {
            response.put("success", false);
            response.put("message", "Mã OTP không tồn tại hoặc đã hết hạn. Vui lòng gửi lại mã.");
            return ResponseEntity.badRequest().body(response);
        }

        if (Instant.now().isAfter(expireAt)) {
            session.removeAttribute("BOOKING_EMAIL_OTP");
            session.removeAttribute("BOOKING_EMAIL_OTP_EMAIL");
            session.removeAttribute("BOOKING_EMAIL_OTP_EXPIRE_AT");

            response.put("success", false);
            response.put("message", "Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
            return ResponseEntity.badRequest().body(response);
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (!savedEmail.equals(normalizedEmail)) {
            response.put("success", false);
            response.put("message", "Email xác thực không khớp với email đã gửi OTP.");
            return ResponseEntity.badRequest().body(response);
        }

        if (otp == null || !savedOtp.equals(otp.trim())) {
            response.put("success", false);
            response.put("message", "Mã OTP không chính xác.");
            return ResponseEntity.badRequest().body(response);
        }

        session.setAttribute("BOOKING_EMAIL_VERIFIED", true);
        session.setAttribute("BOOKING_EMAIL_VERIFIED_EMAIL", normalizedEmail);

        session.removeAttribute("BOOKING_EMAIL_OTP");
        session.removeAttribute("BOOKING_EMAIL_OTP_EMAIL");
        session.removeAttribute("BOOKING_EMAIL_OTP_EXPIRE_AT");

        response.put("success", true);
        response.put("message", "Xác thực email thành công.");

        return ResponseEntity.ok(response);
    }

    private String renderBookingConfirm(
            BookingConfirmRequest request,
            Model model
    ) {
        BookingConfirmView confirmView =
                onlineBookingService.prepareConfirmView(request);

        model.addAttribute("request", request);
        model.addAttribute("confirmView", confirmView);

        return "guest/BookingConfirm";
    }

    private String generateBookingOtp() {
        SecureRandom random = new SecureRandom();
        int number = random.nextInt(900000) + 100000;
        return String.valueOf(number);
    }
}