package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Receptionist;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.BookingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/receptionist/check-in")
public class CheckInController {

    private final BookingService bookingService;

    public CheckInController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public String checkInQueue(@RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {

        if (page < 0) {
            page = 0;
        }

        int pageSize = 5;
        Pageable pageable = PageRequest.of(page, pageSize);

        Page<BookingResponse> bookingPage =
                bookingService.getConfirmedBookingsForCheckIn(keyword, pageable);

        model.addAttribute("bookingPage", bookingPage);
        model.addAttribute("bookings", bookingPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookingPage.getTotalPages());
        model.addAttribute("totalItems", bookingPage.getTotalElements());

        return "receptionist/CheckInQueue";
    }

}