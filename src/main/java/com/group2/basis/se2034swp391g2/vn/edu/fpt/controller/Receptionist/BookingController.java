package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Receptionist;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CheckInProcedureResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CountryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.BookingService;
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

@Controller
@RequestMapping("/receptionist/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final CountryRepository countryRepository;

    public BookingController(BookingService bookingService,
                             CountryRepository countryRepository) {
        this.bookingService = bookingService;
        this.countryRepository = countryRepository;
    }

    @GetMapping
    public String listBookings(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
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
        
        model.addAttribute("countries", countryRepository.findAll());
        model.addAttribute("request", request);
        model.addAttribute("availableRooms", bookingService.getAvailableRooms(checkInDate, checkOutDate));

        return "receptionist/AddWalkInBooking";
    }

    @PostMapping("/add-walk-in")
    public String addWalkInBooking(@ModelAttribute BookingCreateRequest request,
                                   RedirectAttributes redirectAttributes) {

        Long bookingId = bookingService.addWalkInBooking(request);

        if ("create-check-in".equals(request.getAction())) {
            return "redirect:/receptionist/bookings/" + bookingId + "/check-in-complete";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Walk-in booking created successfully."
        );

        return "redirect:/receptionist/bookings";
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

}