package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Admin;


import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminRoomController {

    private final RoomService roomService;

    public AdminRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/admin/listroom")
    public String listRooms(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        return "admin/ListRoom";
    }

    @GetMapping("/admin/rooms/add")
    public String showAddRoomForm() {
        return "admin/AddRoom";
    }

    @GetMapping("/admin/rooms/edit/{id}")
    public String showEditRoomForm() {
        return "admin/EditRoom";
    }

    @GetMapping("/admin/rooms/view/{id}")
    public String viewRoomDetail() {
        return "admin/ViewRoomDetail";
    }
}