package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Receptionist;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/receptionist/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public String listBookings(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                               Model model) {

        model.addAttribute("bookings", bookingService.searchBookings(keyword, status, checkIn, checkOut));
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);

        return "receptionist/ListBooking";
    }

    @GetMapping("/add-walk-in")
    public String showAddWalkInBookingForm(@RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
                                           Model model) {

        if (checkInDate == null) {
            checkInDate = LocalDate.now();
        }

        if (checkOutDate == null) {
            checkOutDate = checkInDate.plusDays(1);
        }

        BookingCreateRequest request = new BookingCreateRequest();
        request.setCheckInDate(checkInDate);
        request.setCheckOutDate(checkOutDate);
        request.setAdults(1);
        request.setChildren(0);

        model.addAttribute("request", request);
        model.addAttribute("availableRooms", bookingService.getAvailableRooms(checkInDate, checkOutDate));

        return "receptionist/AddWalkInBooking";
    }

    @PostMapping("/add-walk-in")
    public String addWalkInBooking(@ModelAttribute BookingCreateRequest request) {
        bookingService.addWalkInBooking(request);
        return "redirect:/receptionist/bookings";
    }
}