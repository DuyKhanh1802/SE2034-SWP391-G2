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
import org.springframework.data.domain.PageRequest;
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
            @RequestParam(required = false) String variantIds,

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

        try {
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

            long nights = bookingSelectionService.calculateNights(checkInDate, checkOutDate);

            BigDecimal roomSubtotal =
                    bookingSelectionService.calculateRoomSubtotal(selectedRooms, nights);

            BigDecimal serviceCharge = roomSubtotal.multiply(BigDecimal.valueOf(0.05));
            BigDecimal vat = roomSubtotal.add(serviceCharge).multiply(BigDecimal.valueOf(0.08));

            BigDecimal promotionDiscountAmount = BigDecimal.ZERO;
            String promotionMessage = "";
            boolean promotionValid = false;
            PromotionApplyResponse promotionResult = null;

            if (promoCode != null && !promoCode.trim().isEmpty()) {
                promotionResult = promotionService.checkPromotionCode(promoCode);

                promotionMessage = promotionResult.getMessage();
                promotionValid = promotionResult.isValid();

                if (promotionResult.isValid() && promotionResult.getDiscountAmount() != null) {
                    promotionDiscountAmount = promotionResult.getDiscountAmount();
                }
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
            model.addAttribute("promotionMessage", promotionMessage);
            model.addAttribute("promotionValid", promotionValid);

            model.addAttribute("roomSubtotal", roomSubtotal);
            model.addAttribute("serviceCharge", serviceCharge);
            model.addAttribute("vat", vat);
            model.addAttribute("grandTotal", grandTotal);

            return "guest/BookingServices";

        } catch (IllegalArgumentException e) {
            Page<BookingServiceProjection> emptyPage =
                    Page.empty(PageRequest.of(0, 6));

            model.addAttribute("errorMessage", e.getMessage());

            model.addAttribute("variantIds", variantIds);
            model.addAttribute("selectedRooms", List.of());

            model.addAttribute("services", List.of());
            model.addAttribute("servicePage", emptyPage);

            model.addAttribute("checkInDate", checkInDate);
            model.addAttribute("checkOutDate", checkOutDate);
            model.addAttribute("nights", 0);

            model.addAttribute("adults", adults);
            model.addAttribute("children", children);
            model.addAttribute("roomCount", roomCount);
            model.addAttribute("roomGuests", roomGuests);
            model.addAttribute("promoCode", promoCode);

            model.addAttribute("selectedCategory", selectedCategory);
            model.addAttribute("selectedSort", selectedSort);
            model.addAttribute("selectedPriceFilter", selectedPriceFilter);

            model.addAttribute("promotionResult", null);
            model.addAttribute("promotionDiscountAmount", BigDecimal.ZERO);
            model.addAttribute("promotionMessage", "");
            model.addAttribute("promotionValid", false);

            model.addAttribute("roomSubtotal", BigDecimal.ZERO);
            model.addAttribute("serviceCharge", BigDecimal.ZERO);
            model.addAttribute("vat", BigDecimal.ZERO);
            model.addAttribute("grandTotal", BigDecimal.ZERO);

            return "guest/BookingServices";
        }
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
            @RequestParam(required = false) String variantIds,

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
            BookingCompleteResult result =
                    onlineBookingService.completeOnlineBooking(request);


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

    private String renderBookingConfirm(
            BookingConfirmRequest request,
            Model model
    ) {
        try {
            BookingConfirmView confirmView =
                    onlineBookingService.prepareConfirmView(request);

            model.addAttribute("request", request);
            model.addAttribute("confirmView", confirmView);

            return "guest/BookingConfirm";

        } catch (IllegalArgumentException e) {
            model.addAttribute("request", request);
            model.addAttribute("errorMessage", e.getMessage());

            return "guest/BookingConfirm";
        }
    }
}