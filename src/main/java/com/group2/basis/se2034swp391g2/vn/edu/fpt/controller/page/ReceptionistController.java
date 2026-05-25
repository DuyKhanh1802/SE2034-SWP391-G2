package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.page;

// ...existing imports...
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class ReceptionistController {
    
    @GetMapping("/receptionist/dashboard")
    public String dashboard(Model model) {
        // fake data - replace with services later
        model.addAttribute("checkinCount", 3);
        model.addAttribute("checkoutCount", 1);
        model.addAttribute("occupied", 12);

        List<Map<String,Object>> bookings = List.of(
            Map.of("id","B001","guestName","Nguyen Van A","roomNumber","101","status","Booked"),
            Map.of("id","B002","guestName","Tran Thi B","roomNumber","203","status","Checked-in")
        );
        model.addAttribute("bookings", bookings);

        return "Receptionist/Dashboard";
    }
}
