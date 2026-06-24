package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Receptionist;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.FinancialChargeSetting;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CheckInProcedureResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CountryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.FinancialChargeSettingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.BookingService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PaymentService;

import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
@Controller
@RequestMapping("/receptionist/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CountryRepository countryRepository;
    private final ServiceRepository serviceRepository;
    private final FinancialChargeSettingRepository financialChargeSettingRepository;
    public BookingController(BookingService bookingService,
                             CountryRepository countryRepository,
                             ServiceRepository serviceRepository,
                             FinancialChargeSettingRepository financialChargeSettingRepository,
                             PaymentService paymentService) {
        this.bookingService = bookingService;
        this.countryRepository = countryRepository;
        this.serviceRepository = serviceRepository;
        this.financialChargeSettingRepository = financialChargeSettingRepository;
        this.paymentService = paymentService;
    }

    @GetMapping
    public String listBookings(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false)
                               @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate checkIn,
                               @RequestParam(required = false)
                               @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate checkOut,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {

        if (page < 0) {
            page = 0;
        }

        int pageSize = 5;
        Pageable pageable = PageRequest.of(page, pageSize);

        Page<BookingResponse> bookingPage =
                bookingService.searchBookingsPaging(keyword, status, checkIn, checkOut, pageable);

        model.addAttribute("bookingPage", bookingPage);
        model.addAttribute("bookings", bookingPage.getContent());

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookingPage.getTotalPages());
        model.addAttribute("totalItems", bookingPage.getTotalElements());

        return "receptionist/ListBooking";
    }

    @GetMapping("/add-walk-in")
    public String showAddWalkInBookingForm(@RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
                                           @RequestParam(required = false) Integer adults,
                                           @RequestParam(required = false) Integer children,
                                           @RequestParam(required = false) List<Integer> childAges,
                                           Model model) {

        BookingCreateRequest request = new BookingCreateRequest();

        request.setCheckInDate(checkInDate);
        request.setCheckOutDate(checkOutDate);
        request.setAdults(adults);
        request.setChildren(children == null ? 0 : children);
        request.setChildAges(childAges);

        model.addAttribute("countries", countryRepository.findAll());
        model.addAttribute("request", request);
        model.addAttribute("diningServices", serviceRepository.findAvailableByCategoryId(1L));
        model.addAttribute("wellnessServices", serviceRepository.findAvailableByCategoryId(2L));

        FinancialChargeSetting setting = financialChargeSettingRepository
                .findCurrentSetting(LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("Chưa cấu hình thuế và phí dịch vụ."));

        model.addAttribute("vatRate", setting.getVatRate());
        model.addAttribute("serviceChargeRate", setting.getServiceChargeRate());
        model.addAttribute("taxOnServiceCharge", setting.getTaxOnServiceCharge());
        model.addAttribute("priceDisplayMode", setting.getPriceDisplayMode());

        boolean hasApplied =
                checkInDate != null
                        && checkOutDate != null
                        && adults != null;

        model.addAttribute("hasApplied", hasApplied);

        if (hasApplied) {
            try {
                model.addAttribute(
                        "availableRooms",
                        bookingService.getAvailableRooms(checkInDate, checkOutDate)
                );
            } catch (IllegalArgumentException e) {
                model.addAttribute("errorMessage", e.getMessage());
                model.addAttribute("availableRooms", java.util.Collections.emptyList());
            }
        } else {
            model.addAttribute("availableRooms", java.util.Collections.emptyList());
        }

        return "receptionist/AddWalkInBooking";
    }

    @PostMapping("/add-walk-in")
    public String addWalkInBooking(@ModelAttribute BookingCreateRequest request,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        try {
            Long bookingId = bookingService.addWalkInBooking(request);

            if ("create-check-in".equals(request.getAction())) {
                return "redirect:/receptionist/bookings/" + bookingId + "/check-in-complete";
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Walk-in booking created successfully."
            );

            return "redirect:/receptionist/bookings";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("countries", countryRepository.findAll());
            model.addAttribute("request", request);

            model.addAttribute("diningServices", serviceRepository.findAvailableByCategoryId(1L));
            model.addAttribute("wellnessServices", serviceRepository.findAvailableByCategoryId(2L));

            FinancialChargeSetting setting = financialChargeSettingRepository
                    .findCurrentSetting(LocalDate.now())
                    .orElseThrow(() -> new IllegalArgumentException("Chưa cấu hình thuế và phí dịch vụ."));

            model.addAttribute("vatRate", setting.getVatRate());
            model.addAttribute("serviceChargeRate", setting.getServiceChargeRate());
            model.addAttribute("taxOnServiceCharge", setting.getTaxOnServiceCharge());
            model.addAttribute("priceDisplayMode", setting.getPriceDisplayMode());

            try {
                model.addAttribute(
                        "availableRooms",
                        bookingService.getAvailableRooms(request.getCheckInDate(), request.getCheckOutDate())
                );
            } catch (IllegalArgumentException ignored) {
                model.addAttribute("availableRooms", java.util.Collections.emptyList());
            }

            return "receptionist/AddWalkInBooking";
        }
    }

    @GetMapping("/{bookingId}/check-in-complete")
    public String showCheckInComplete(@PathVariable Long bookingId,
                                      Model model) {

        model.addAttribute("roomCodes", bookingService.getCheckInCompleteInfo(bookingId));

        return "receptionist/CheckInComplete";
    }

    @GetMapping("/{bookingId}/check-in")
    public String showCheckInProcedure(@PathVariable Long bookingId,
                                       Model model) {

        CheckInProcedureResponse checkInInfo = bookingService.getCheckInProcedure(bookingId);

        model.addAttribute("checkInInfo", checkInInfo);
        model.addAttribute("assignedRooms", bookingService.getAssignedRoomsForCheckIn(bookingId));
        model.addAttribute("nights",
                java.time.temporal.ChronoUnit.DAYS.between(
                        checkInInfo.getCheckInDate(),
                        checkInInfo.getCheckOutDate()
                )
        );

        return "receptionist/CheckInProcedure";
    }
    @PostMapping("/{bookingId}/confirm-check-in")
    public String confirmCheckIn(@PathVariable Long bookingId) {
        Long checkedInBookingId = bookingService.confirmCheckIn(bookingId);
        return "redirect:/receptionist/bookings/" + checkedInBookingId + "/check-in-complete";
    }

    @PostMapping("/booking-details/{bookingDetailId}/send-room-code")
    public String sendRoomCode(@PathVariable Long bookingDetailId,
                               @RequestParam Long bookingId,
                               RedirectAttributes redirectAttributes) {
        try {
            bookingService.sendRoomCodeEmail(bookingDetailId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Room code has been sent to guest email."
            );
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/receptionist/bookings/" + bookingId + "/check-in-complete";
    }

    @PostMapping("/{bookingId}/send-room-codes")
    public String sendRoomCodes(@PathVariable Long bookingId,
                                RedirectAttributes redirectAttributes) {
        try {
            bookingService.sendRoomCodesEmail(bookingId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Room code email has been sent to the guest."
            );
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/receptionist/bookings/" + bookingId + "/check-in-complete";
    }

    @GetMapping("/view/{bookingId}")
    public String viewBookingDetail(@PathVariable Long bookingId, Model model){
        model.addAttribute("detail",bookingService.getBookingDetail(bookingId));
        return "receptionist/ViewBookingDetail";
    }

    @GetMapping("/edit/{bookingId}")
    public String showEditBookingForm(@PathVariable Long bookingId,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("pageTitle", "CHỈNH SỬA THÔNG TIN KHÁCH");
            model.addAttribute("bookingId", bookingId);
            model.addAttribute("request", bookingService.getBookingUpdateForm(bookingId));
            model.addAttribute("detail", bookingService.getBookingDetail(bookingId));
            model.addAttribute("countries", countryRepository.findByIsActiveTrueOrderByCountryNameAsc());

            return "receptionist/EditBooking";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/receptionist/bookings/view/" + bookingId;
        }
    }

    @PostMapping("/edit/{bookingId}")
    public String updateBooking(@PathVariable Long bookingId,
                                @ModelAttribute("request") BookingUpdateRequest request,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            bookingService.updateBookingGuestInfo(bookingId, request);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin khách thành công.");
            return "redirect:/receptionist/bookings/view/" + bookingId;

        } catch (IllegalArgumentException e) {
            model.addAttribute("pageTitle", "CHỈNH SỬA THÔNG TIN KHÁCH");
            model.addAttribute("bookingId", bookingId);
            model.addAttribute("request", request);
            model.addAttribute("detail", bookingService.getBookingDetail(bookingId));
            model.addAttribute("countries", countryRepository.findByIsActiveTrueOrderByCountryNameAsc());
            model.addAttribute("errorMessage", e.getMessage());

            return "receptionist/EditBooking";
        }
    }

    @PostMapping("/{bookingId}/payments")
    public String createAdvancePayment(@PathVariable Long bookingId,
                                       @RequestParam PaymentType paymentType,
                                       @RequestParam PaymentMethod method,
                                       @RequestParam BigDecimal amount,
                                       RedirectAttributes redirectAttributes) {
        try {
            if (paymentType != PaymentType.DEPOSIT && paymentType != PaymentType.FULL) {
                throw new IllegalArgumentException("Màn chi tiết đặt phòng chỉ cho phép thu cọc hoặc thu full trước.");
            }

            Booking booking = bookingService.getBookingEntityById(bookingId);
            User currentStaff = bookingService.getCurrentStaffUser();

            paymentService.createPayment(
                    booking,
                    paymentType,
                    method,
                    amount,
                    currentStaff
            );

            if (paymentType == PaymentType.DEPOSIT) {
                bookingService.markDepositPaid(bookingId);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Lưu thanh toán thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/receptionist/bookings/view/" + bookingId;
    }
}
