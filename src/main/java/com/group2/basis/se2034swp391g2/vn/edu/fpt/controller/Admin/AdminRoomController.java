package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Admin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CloudinaryService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final ProfileService profileService;

    public AdminRoomController(RoomService roomService,
                               CloudinaryService cloudinaryService,
                               ProfileService profileService) {
        this.roomService = roomService;
        this.cloudinaryService = cloudinaryService;
        this.profileService = profileService;
    }

    @GetMapping("/admin/list_room")
    public String listRooms(Model model,
                            Authentication authentication,
                            HttpSession session) {

        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("rooms", roomService.getAllRooms());

        return "admin/ListRoom";
    }

    @GetMapping("/admin/list_room/add")
    public String showAddRoomForm(Model model,
                                  Authentication authentication,
                                  HttpSession session) {

        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("roomTypes", roomService.getAllRoomTypes());
        model.addAttribute("viewTypes", ViewType.values());
        model.addAttribute("roomStatuses", RoomStatus.values());

        return "admin/AddRoom";
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
                          RedirectAttributes redirectAttributes,
                          Authentication authentication,
                          HttpSession session) {

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
            User currentUser = profileService.resolveCurrentUser(authentication, session);

            model.addAttribute("currentUser", currentUser);
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

    @PostMapping("/admin/room-images/upload")
    @ResponseBody
    public Map<String, String> uploadRoomImage(@RequestParam("file") MultipartFile file) {
        Map uploadResult = cloudinaryService.uploadRoomImage(file);

        return Map.of(
                "imageUrl", uploadResult.get("secure_url").toString()
        );
    }

    @GetMapping("/admin/rooms/edit/{id}")
    public String showEditRoomForm(@PathVariable Long id,
                                   Model model,
                                   Authentication authentication,
                                   HttpSession session) {

        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("room", roomService.getRoomById(id));
        model.addAttribute("roomTypes", roomService.getAllRoomTypes());
        model.addAttribute("viewTypes", ViewType.values());
        model.addAttribute("roomStatuses", RoomStatus.values());

        return "admin/EditRoom";
    }

    @PostMapping("/admin/rooms/edit/{id}")
    public String updateRoom(@PathVariable Long id,
                             @RequestParam String roomNumber,
                             @RequestParam Long roomTypeId,
                             @RequestParam Integer floor,
                             @RequestParam ViewType viewType,
                             @RequestParam RoomStatus status,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication,
                             HttpSession session) {

        try {
            roomService.updateRoom(
                    id,
                    roomNumber,
                    roomTypeId,
                    floor,
                    viewType,
                    status
            );

            redirectAttributes.addFlashAttribute("successMessage", "Update room successfully!");

            return "redirect:/admin/list_room";

        } catch (IllegalArgumentException e) {
            User currentUser = profileService.resolveCurrentUser(authentication, session);

            model.addAttribute("currentUser", currentUser);
            model.addAttribute("errorMessage", e.getMessage());

            model.addAttribute("room", roomService.getRoomById(id));
            model.addAttribute("roomTypes", roomService.getAllRoomTypes());
            model.addAttribute("viewTypes", ViewType.values());
            model.addAttribute("roomStatuses", RoomStatus.values());

            return "admin/EditRoom";
        }
    }

    @GetMapping("/admin/rooms/view/{id}")
    public String viewRoomDetail(@PathVariable Long id,
                                 Model model,
                                 Authentication authentication,
                                 HttpSession session) {

        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("room", roomService.getRoomById(id));

        return "admin/ViewRoomDetail";
    }
}