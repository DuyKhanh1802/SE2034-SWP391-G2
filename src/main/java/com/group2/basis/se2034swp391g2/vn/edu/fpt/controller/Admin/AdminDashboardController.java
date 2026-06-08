package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Admin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    private final RoomService roomService;

    public AdminDashboardController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/admin/rooms")
    public String listRooms(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        return "admin/ListRoom";
    }
}