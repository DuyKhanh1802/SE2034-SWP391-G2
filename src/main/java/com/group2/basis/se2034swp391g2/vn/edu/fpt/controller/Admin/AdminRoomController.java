package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Admin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CloudinaryService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class AdminRoomController {

    private final RoomService roomService;
    private final CloudinaryService cloudinaryService;

    public AdminRoomController(RoomService roomService, CloudinaryService cloudinaryService) {
        this.roomService = roomService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/admin/list_room")
    public String listRooms(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        return "admin/ListRoom";
    }

    @GetMapping("/admin/list_room/add")
    public String showAddRoomForm(Model model) {
        model.addAttribute("roomTypes", roomService.getAllRoomTypes());
        model.addAttribute("viewTypes", ViewType.values());
        model.addAttribute("roomStatuses", RoomStatus.values());
        return "admin/AddRoom";
    }

    @PostMapping("/admin/room-images/upload")
    @ResponseBody
    public Map<String, String> uploadRoomImage(@RequestParam("file") MultipartFile file) {
        Map uploadResult = cloudinaryService.uploadRoomImage(file);

        return Map.of(
                "imageUrl", uploadResult.get("secure_url").toString()
        );
    }

    @PostMapping("/admin/list_room/add")
    public String addRoom(@RequestParam String roomNumber,
                          @RequestParam Long roomTypeId,
                          @RequestParam Integer floor,
                          @RequestParam ViewType viewType,
                          @RequestParam RoomStatus status,
                          @RequestParam(required = false) List<String> imageUrls,
                          @RequestParam(defaultValue = "0") Integer primaryImageIndex,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        try {
            roomService.createRoom(
                    roomNumber,
                    roomTypeId,
                    floor,
                    viewType,
                    status,
                    imageUrls,
                    primaryImageIndex
            );

            redirectAttributes.addFlashAttribute("successMessage", "Add room successfully!");

            return "redirect:/admin/list_room";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());

            model.addAttribute("roomTypes", roomService.getAllRoomTypes());
            model.addAttribute("viewTypes", ViewType.values());
            model.addAttribute("roomStatuses", RoomStatus.values());

            model.addAttribute("roomNumber", roomNumber);
            model.addAttribute("selectedRoomTypeId", roomTypeId);
            model.addAttribute("floor", floor);
            model.addAttribute("selectedViewType", viewType);
            model.addAttribute("selectedStatus", status);

            return "admin/AddRoom";
        }
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