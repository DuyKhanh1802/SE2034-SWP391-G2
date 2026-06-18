package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.HotelAdmin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CloudinaryService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
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
public class HotelAdminRoomController {

    private final RoomService roomService;
    private final CloudinaryService cloudinaryService;
    private final ProfileService profileService;

    public HotelAdminRoomController(RoomService roomService,
                                    CloudinaryService cloudinaryService,
                                    ProfileService profileService) {
        this.roomService = roomService;
        this.cloudinaryService = cloudinaryService;
        this.profileService = profileService;
    }

    private void addLayoutData(Model model,
                               Authentication authentication,
                               HttpSession session,
                               HttpServletRequest request,
                               String pageTitle) {
        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("pageTitle", pageTitle);
    }

    @GetMapping("/hotel-admin/list-room")
    public String listRooms(@RequestParam(defaultValue = "0") int page,
                            Model model,
                            Authentication authentication,
                            HttpSession session,
                            HttpServletRequest request) {

        addLayoutData(model, authentication, session, request, "Danh sách phòng");

        int pageSize = 5;
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Room> roomPage = roomService.getRoomsPage(pageable);

        model.addAttribute("rooms", roomPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", roomPage.getTotalPages());
        model.addAttribute("totalItems", roomPage.getTotalElements());
        model.addAttribute("pageSize", pageSize);

        return "hotel_admin/ListRoom";
    }

    @GetMapping("/hotel-admin/list-room/add")
    public String showAddRoomForm(Model model,
                                  Authentication authentication,
                                  HttpSession session,
                                  HttpServletRequest request) {

        addLayoutData(model, authentication, session, request, "Thêm phòng");

        model.addAttribute("roomTypeVariants", roomService.getAllRoomTypeVariants());
        model.addAttribute("roomNumberOptions", roomService.getAvailableRoomNumberOptions());
        model.addAttribute("viewTypes", ViewType.values());
        model.addAttribute("roomStatuses", RoomStatus.values());

        return "hotel_admin/AddRoom";
    }

    @PostMapping("/hotel-admin/list-room/add")
    public String addRoom(@RequestParam String roomNumber,
                          @RequestParam Long variantId,
                          @RequestParam(required = false) Integer floor,
                          @RequestParam RoomStatus status,
                          @RequestParam(required = false) String note,
                          @RequestParam(required = false) List<String> imageUrls,
                          @RequestParam(defaultValue = "0") Integer primaryImageIndex,
                          Model model,
                          RedirectAttributes redirectAttributes,
                          Authentication authentication,
                          HttpSession session,
                          HttpServletRequest request) {

        try {
            roomService.createRoom(
                    roomNumber,
                    variantId,
                    floor,
                    status,
                    note,
                    imageUrls,
                    primaryImageIndex
            );

            redirectAttributes.addFlashAttribute("successMessage", "Thêm phòng thành công!");

            return "redirect:/hotel-admin/list-room";

        } catch (IllegalArgumentException e) {
            addLayoutData(model, authentication, session, request, "Thêm phòng");

            model.addAttribute("errorMessage", e.getMessage());

            model.addAttribute("roomTypeVariants", roomService.getAllRoomTypeVariants());
            model.addAttribute("roomNumberOptions", roomService.getAvailableRoomNumberOptions());
            model.addAttribute("viewTypes", ViewType.values());
            model.addAttribute("roomStatuses", RoomStatus.values());

            model.addAttribute("roomNumber", roomNumber);
            model.addAttribute("selectedVariantId", variantId);
            model.addAttribute("floor", floor);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("note", note);

            return "hotel_admin/AddRoom";
        }
    }

    @PostMapping("/hotel-admin/room-images/upload")
    @ResponseBody
    public Map<String, String> uploadRoomImage(@RequestParam("file") MultipartFile file) {
        Map uploadResult = cloudinaryService.uploadRoomImage(file);

        return Map.of(
                "imageUrl", uploadResult.get("secure_url").toString()
        );
    }

    @GetMapping("/hotel-admin/rooms/edit/{id}")
    public String showEditRoomForm(@PathVariable Long id,
                                   Model model,
                                   Authentication authentication,
                                   HttpSession session,
                                   HttpServletRequest request) {

        addLayoutData(model, authentication, session, request, "Chỉnh sửa phòng");

        model.addAttribute("room", roomService.getRoomById(id));
        model.addAttribute("roomTypeVariants", roomService.getAllRoomTypeVariants());
        model.addAttribute("viewTypes", ViewType.values());
        model.addAttribute("roomStatuses", RoomStatus.values());

        return "hotel_admin/EditRoom";
    }

    @PostMapping("/hotel-admin/rooms/edit/{id}")
    public String updateRoom(@PathVariable Long id,
                             @RequestParam String roomNumber,
                             @RequestParam Long variantId,
                             @RequestParam(required = false) Integer floor,
                             @RequestParam RoomStatus status,
                             @RequestParam(required = false) String note,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication,
                             HttpSession session,
                             HttpServletRequest request) {

        try {
            roomService.updateRoom(
                    id,
                    roomNumber,
                    variantId,
                    floor,
                    status,
                    note
            );

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phòng thành công!");

            return "redirect:/hotel-admin/list-room";

        } catch (IllegalArgumentException e) {
            addLayoutData(model, authentication, session, request, "Chỉnh sửa phòng");

            model.addAttribute("errorMessage", e.getMessage());

            model.addAttribute("room", roomService.getRoomById(id));
            model.addAttribute("roomTypeVariants", roomService.getAllRoomTypeVariants());
            model.addAttribute("viewTypes", ViewType.values());
            model.addAttribute("roomStatuses", RoomStatus.values());

            return "hotel_admin/EditRoom";
        }
    }

    @PostMapping("/hotel-admin/rooms/delete/{id}")
    public String deleteRoom(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {

        roomService.deleteRoom(id);

        redirectAttributes.addFlashAttribute("successMessage", "Xóa phòng thành công!");

        return "redirect:/hotel-admin/list-room";
    }

    @GetMapping("/hotel-admin/rooms/view/{id}")
    public String viewRoomDetail(@PathVariable Long id,
                                 Model model,
                                 Authentication authentication,
                                 HttpSession session,
                                 HttpServletRequest request) {

        addLayoutData(model, authentication, session, request, "Chi tiết phòng");

        model.addAttribute("room", roomService.getRoomById(id));
        model.addAttribute("roomImages", roomService.getRoomImages(id));

        return "hotel_admin/ViewRoomDetail";
    }
}