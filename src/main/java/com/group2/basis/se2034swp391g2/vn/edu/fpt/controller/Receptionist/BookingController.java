package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Receptionist;


import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CheckInProcedureResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CountryRepository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.BookingService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CheckoutService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.CheckoutRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PaymentService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PromotionService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomMoveFeePolicy;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomMoveReason;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.RoomMoveRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RoomMoveService;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionException;
import java.time.LocalDate;
import java.util.List;
@Controller
@RequestMapping("/receptionist/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CountryRepository countryRepository;
    private final ServiceRepository serviceRepository;
    private final PromotionService promotionService;
    private final RoomMoveService roomMoveService;
    private final CheckoutService checkoutService;
    public BookingController(BookingService bookingService,
                             CountryRepository countryRepository,
                             PromotionService promotionService,
                             ServiceRepository serviceRepository,
                             RoomMoveService roomMoveService,
                             CheckoutService checkoutService,
                             PaymentService paymentService) {
        this.bookingService = bookingService;
        this.countryRepository = countryRepository;
        this.serviceRepository = serviceRepository;
        this.promotionService = promotionService;
        this.paymentService = paymentService;
        this.roomMoveService = roomMoveService;
        this.checkoutService = checkoutService;
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
        model.addAttribute("pageTitle", "Quản lý đặt phòng");
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

        loadAddWalkInPage(model, request, null);

        return "receptionist/AddWalkInBooking";
    }

    @PostMapping("/add-walk-in")
    public String addWalkInBooking(@ModelAttribute("request") BookingCreateRequest request,
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
            loadAddWalkInPage(model, request, e.getMessage());
            return "receptionist/AddWalkInBooking";
        }
    }

    private void loadAddWalkInPage(Model model,
                                   BookingCreateRequest request,
                                   String errorMessage) {

        if (request.getChildren() == null) {
            request.setChildren(0);
        }

        model.addAttribute("request", request);

        model.addAttribute("countries", countryRepository.findAll());
        model.addAttribute("diningServices", serviceRepository.findAvailableByCategoryId(1L));
        model.addAttribute("wellnessServices", serviceRepository.findAvailableByCategoryId(2L));
        model.addAttribute("activePromotions", promotionService.getListPromotion());
        model.addAttribute("vatRate", new BigDecimal("8"));
        model.addAttribute("serviceChargeRate", new BigDecimal("5"));
        model.addAttribute("taxOnServiceCharge", true);
        model.addAttribute("priceDisplayMode", "EXCLUSIVE");

        boolean hasApplied =
                request.getCheckInDate() != null
                        && request.getCheckOutDate() != null
                        && request.getAdults() != null;

        model.addAttribute("hasApplied", hasApplied);

        if (hasApplied) {
            try {
                model.addAttribute(
                        "availableRooms",
                        bookingService.getAvailableRooms(
                                request.getCheckInDate(),
                                request.getCheckOutDate()
                        )
                );
            } catch (IllegalArgumentException roomError) {
                model.addAttribute("availableRooms", java.util.Collections.emptyList());

                if (errorMessage == null || errorMessage.isBlank()) {
                    errorMessage = roomError.getMessage();
                }
            }
        } else {
            model.addAttribute("availableRooms", java.util.Collections.emptyList());
        }

        if (errorMessage != null && !errorMessage.isBlank()) {
            model.addAttribute("errorMessage", errorMessage);
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
                    "Email mã phòng đã được gửi cho khách."
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
    public String viewBookingDetail(@PathVariable Long bookingId, Model model) {
        model.addAttribute("detail", bookingService.getBookingDetail(bookingId));
        model.addAttribute(
                "availableRoomsByDetail",
                bookingService.getAvailableRoomsForPendingBooking(bookingId)
        );
        model.addAttribute(
                "roomMoveOptionsByDetail",
                roomMoveService.getAvailableRoomsByDetail(bookingId)
        );
        model.addAttribute("roomMoveHistory", roomMoveService.getRoomMoveHistory(bookingId));
        model.addAttribute("roomMoveReasons", RoomMoveReason.values());
        model.addAttribute("roomMoveFeePolicies", RoomMoveFeePolicy.values());
        model.addAttribute("roomMoveOldRoomStatuses", List.of(RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE));
        return "receptionist/ViewBookingDetail";
    }

    @PostMapping("/view/{bookingId}/room-move")
    public String moveRoom(@PathVariable Long bookingId,
                           @ModelAttribute RoomMoveRequest request,
                           RedirectAttributes redirectAttributes) {
        try {
            request.setBookingId(bookingId);
            roomMoveService.moveRoom(request);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đổi phòng thành công. Nếu có phụ thu nâng hạng, khoản phí đã được ghi nhận vào folio."
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/receptionist/bookings/view/" + bookingId;
    }

    @PostMapping("/view/{bookingId}/services/{folioItemId}/confirm")
    public String confirmServiceServed(@PathVariable Long bookingId,
                                       @PathVariable Long folioItemId,
                                       RedirectAttributes redirectAttributes) {
        try {
            bookingService.confirmServiceServed(bookingId, folioItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận phục vụ dịch vụ và ghi nhận tiêu hao kho.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/receptionist/bookings/view/" + bookingId;
    }

    @PostMapping("/view/{bookingId}/services/{folioItemId}/cancel")
    public String cancelRequestedService(@PathVariable Long bookingId,
                                         @PathVariable Long folioItemId,
                                         RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelRequestedService(bookingId, folioItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy dịch vụ chờ phục vụ.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/receptionist/bookings/view/" + bookingId;
    }

    @PostMapping("/view/{bookingId}/services/{folioItemId}/not-used")
    public String markServiceNotUsedNoRefund(@PathVariable Long bookingId,
                                             @PathVariable Long folioItemId,
                                             RedirectAttributes redirectAttributes) {
        try {
            bookingService.markServiceNotUsedNoRefund(bookingId, folioItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận dịch vụ không sử dụng và không hoàn tiền.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/receptionist/bookings/view/" + bookingId;
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
            if (paymentType != PaymentType.FULL) {
                throw new IllegalArgumentException("Màn chi tiết đặt phòng chỉ cho phép thu toàn bộ số tiền còn lại.");
            }

            bookingService.collectBookingPayment(
                    bookingId,
                    paymentType,
                    method,
                    amount
            );

            redirectAttributes.addFlashAttribute("successMessage", "Lưu thanh toán thành công.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/receptionist/bookings/view/" + bookingId;
    }

    @GetMapping("/{bookingId}/check-out")
    public String showCheckout(@PathVariable Long bookingId,
                               RedirectAttributes redirectAttributes) {
        try {
            Long bookingDetailId = checkoutService.findFirstCheckoutDetailId(bookingId);
            return "redirect:/receptionist/bookings/details/" + bookingDetailId + "/check-out";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/receptionist/bookings/view/" + bookingId;
        }
    }

    @GetMapping("/details/{bookingDetailId}/check-out")
    public String showRoomCheckout(@PathVariable Long bookingDetailId,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("checkout", checkoutService.getCheckoutDetail(bookingDetailId));
            model.addAttribute("request", new CheckoutRequest());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("pageTitle", "Trả phòng");
            return "receptionist/Checkout";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/receptionist/rooms";
        }
    }

    @PostMapping("/details/{bookingDetailId}/check-out")
    public String completeCheckout(@PathVariable Long bookingDetailId,
                                   @ModelAttribute("request") CheckoutRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            checkoutService.completeCheckout(bookingDetailId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Hoàn tất trả phòng thành công.");
            return "redirect:/receptionist/rooms";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/receptionist/bookings/details/" + bookingDetailId + "/check-out";
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hoàn tất trả phòng do lỗi ghi dữ liệu. Vui lòng kiểm tra lại cấu hình thanh toán hoặc thử lại.");
            return "redirect:/receptionist/bookings/details/" + bookingDetailId + "/check-out";
        } catch (TransactionException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hoàn tất trả phòng do lỗi giao dịch dữ liệu. Vui lòng thử lại.");
            return "redirect:/receptionist/bookings/details/" + bookingDetailId + "/check-out";
        }
    }

    @PostMapping("/{bookingId}/cancel")
    public String cancelBooking(@PathVariable Long bookingId,
                                @RequestParam String cancelReason,
                                RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(bookingId, cancelReason);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Hủy đặt phòng thành công."
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/receptionist/bookings/view/" + bookingId;
    }

    @PostMapping("/{bookingId}/no-show")
    public String markNoShow(@PathVariable Long bookingId,
                             @RequestParam String reason,
                             RedirectAttributes redirectAttributes) {
        try {
            bookingService.markNoShow(bookingId, reason);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã đánh dấu khách không đến."
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/receptionist/bookings/view/" + bookingId;
    }

    @PostMapping("/{bookingId}/confirm")
    public String confirmBooking(@PathVariable Long bookingId,
                                 @RequestParam(required = false) List<Long> bookingDetailIds,
                                 @RequestParam(required = false) List<String> roomIds,
                                 RedirectAttributes redirectAttributes) {
        try {
            bookingService.confirmOnlineBooking(bookingId, bookingDetailIds, roomIds);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Xác nhận đặt phòng và phân phòng thành công. Email xác nhận đã được gửi cho khách."
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/receptionist/bookings/view/" + bookingId;
    }


}
