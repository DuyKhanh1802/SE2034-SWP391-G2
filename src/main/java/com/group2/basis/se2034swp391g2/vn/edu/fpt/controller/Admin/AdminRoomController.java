package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Admin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CloudinaryService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    // =========================
    // LIST ROOM WITH PAGINATION
    // =========================
    @GetMapping("/admin/list_room")
    public String listRooms(@RequestParam(defaultValue = "0") int page,
                            Model model,
                            Authentication authentication,
                            HttpSession session) {

        User currentUser = profileService.resolveCurrentUser(authentication, session);

        int pageSize = 5;
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Room> roomPage = roomService.getRoomsPage(pageable);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("rooms", roomPage.getContent());

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", roomPage.getTotalPages());
        model.addAttribute("totalItems", roomPage.getTotalElements());
        model.addAttribute("pageSize", pageSize);

        return "admin/ListRoom";
    }

    // =========================
    // SHOW ADD ROOM FORM
    // =========================
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

    // =========================
    // ADD ROOM
    // =========================
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

            redirectAttributes.addFlashAttribute("successMessage", "Thêm phòng thành công!");

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

    // =========================
    // UPLOAD ROOM IMAGE TO CLOUDINARY
    // =========================
    @PostMapping("/admin/room-images/upload")
    @ResponseBody
    public Map<String, String> uploadRoomImage(@RequestParam("file") MultipartFile file) {
        Map uploadResult = cloudinaryService.uploadRoomImage(file);

        return Map.of(
                "imageUrl", uploadResult.get("secure_url").toString()
        );
    }

    // =========================
    // SHOW EDIT ROOM FORM
    // =========================
    @GetMapping("/admin/list_room/edit/{id}")
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

    // =========================
    // UPDATE ROOM
    // =========================
    @PostMapping("/admin/list_room/edit/{id}")
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

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phòng thành công!");

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

    // =========================
    // VIEW ROOM DETAIL
    // =========================
    @GetMapping("/admin/list_room/view/{id}")
    public String viewRoomDetail(@PathVariable Long id,
                                 Model model,
                                 Authentication authentication,
                                 HttpSession session) {

        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("room", roomService.getRoomById(id));
        model.addAttribute("roomImages", roomService.getRoomImages(id));

        return "admin/ViewRoomDetail";
    }

    // =========================
    // DELETE ROOM
    // =========================
    @PostMapping("/admin/list_room/delete/{id}")
    public String deleteRoom(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {

        roomService.deleteRoom(id);

        redirectAttributes.addFlashAttribute("successMessage", "Xóa phòng thành công!");

        return "redirect:/admin/list_room";
    }
}