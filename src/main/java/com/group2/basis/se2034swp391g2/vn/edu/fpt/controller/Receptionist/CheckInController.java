package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Receptionist;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.BookingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;


@Controller
@RequestMapping("/receptionist/check-in")
public class CheckInController {

    private final BookingService bookingService;
    private final RoomTypeRepository roomTypeRepository;

    public CheckInController(BookingService bookingService,
                             RoomTypeRepository roomTypeRepository) {
        this.bookingService = bookingService;
        this.roomTypeRepository = roomTypeRepository;
    }

    @GetMapping
    public String checkInQueue(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Long roomTypeId,
                               @RequestParam(required = false)
                                   @DateTimeFormat(pattern = "dd/MM/yyyy")
                                   LocalDate checkInDate,
                               @RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {

        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(page, 5);

        Page<BookingResponse> bookingPage =
                bookingService.getConfirmedBookingsForCheckIn(
                        keyword, roomTypeId, checkInDate, status, pageable
                );
        model.addAttribute("pageTitle", "Danh sách nhận phòng");
        model.addAttribute("bookingPage", bookingPage);
        model.addAttribute("bookings", bookingPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookingPage.getTotalPages());
        model.addAttribute("totalItems", bookingPage.getTotalElements());

        model.addAttribute("roomTypes", roomTypeRepository.findByIsDeletedFalse());
        model.addAttribute("roomTypeId", roomTypeId);
        model.addAttribute("checkInDate", checkInDate);
        model.addAttribute("status", status);

        return "receptionist/CheckInQueue";
    }
}