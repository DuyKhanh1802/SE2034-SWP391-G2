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
import java.util.Set;

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
    public String listRooms(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String roomType,
                            @RequestParam(required = false) Integer floor,
                            @RequestParam(required = false) String viewType,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String operatingStatus,
                            @RequestParam(defaultValue = "0") Integer page,
                            @RequestParam(defaultValue = "5") Integer size,
                            Model model,
                            Authentication authentication,
                            HttpSession session,
                            HttpServletRequest request) {

        addLayoutData(model, authentication, session, request, "Danh sách phòng");

        String validKeyword = null;
        String localErrorMessage = null;

        try {
            validKeyword = normalizeKeyword(keyword);
        } catch (IllegalArgumentException e) {
            localErrorMessage = e.getMessage();
        }

        String validRoomType = normalizeRoomType(roomType);
        Integer validFloor = normalizeFloor(floor);
        ViewType validViewType = normalizeViewType(viewType);
        RoomStatus validStatus = normalizeRoomStatus(status);
        String validOperatingStatus = normalizeOperatingStatus(operatingStatus);

        int validPage = normalizePage(page);
        int validSize = normalizeSize(size);

        Page<Room> roomPage = roomService.searchRoomsForAdmin(
                validKeyword,
                validRoomType,
                validFloor,
                validViewType,
                validStatus,
                validOperatingStatus,
                validPage,
                validSize
        );

        if (roomPage.isEmpty() && localErrorMessage == null) {
            localErrorMessage = "Không tìm thấy phòng phù hợp với từ khóa tìm kiếm.";
        }

        model.addAttribute("rooms", roomPage.getContent());
        model.addAttribute("currentPage", validPage);
        model.addAttribute("totalPages", roomPage.getTotalPages());
        model.addAttribute("totalItems", roomPage.getTotalElements());
        model.addAttribute("pageSize", validSize);

        model.addAttribute("keyword", validKeyword);
        model.addAttribute("roomType", validRoomType);
        model.addAttribute("floor", validFloor);
        model.addAttribute("viewType", validViewType == null ? null : validViewType.name());
        model.addAttribute("status", validStatus == null ? null : validStatus.name());
        model.addAttribute("operatingStatus", validOperatingStatus);

        model.addAttribute("roomTypeNames", roomService.getRoomTypeNamesForAdminFilter());
        model.addAttribute("floors", roomService.getRoomFloors());
        model.addAttribute("viewTypes", ViewType.values());
        model.addAttribute("roomStatuses", RoomStatus.values());

        if (localErrorMessage != null) {
            model.addAttribute("errorMessage", localErrorMessage);
        }

        return "hotel_admin/ListRoom";
    }

    @GetMapping("/hotel-admin/list-room/add")
    public String showAddRoomForm(@RequestParam(required = false) String roomNumber,
                                  Model model,
                                  Authentication authentication,
                                  HttpSession session,
                                  HttpServletRequest request) {

        addLayoutData(model, authentication, session, request, "Thêm phòng");

        try {
            addAddRoomFormData(model, roomNumber);
        } catch (IllegalArgumentException e) {
            addAddRoomFormData(model, null);
            model.addAttribute("roomNumber", roomNumber);
            model.addAttribute("errorMessage", e.getMessage());
        }

        return "hotel_admin/AddRoom";
    }

    @PostMapping("/hotel-admin/list-room/add")
    public String addRoom(@RequestParam(required = false) String roomNumber,
                          @RequestParam(required = false) Long variantId,
                          @RequestParam(required = false) RoomStatus status,
                          @RequestParam(required = false) String note,
                          Model model,
                          RedirectAttributes redirectAttributes,
                          Authentication authentication,
                          HttpSession session,
                          HttpServletRequest request) {

        try {
            roomService.createRoom(roomNumber, variantId, status, note);

            redirectAttributes.addFlashAttribute("successMessage", "Thêm phòng thành công!");

            return "redirect:/hotel-admin/list-room";

        } catch (IllegalArgumentException e) {
            addLayoutData(model, authentication, session, request, "Thêm phòng");

            try {
                addAddRoomFormData(model, roomNumber);
            } catch (IllegalArgumentException ignored) {
                addAddRoomFormData(model, null);
            }

            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roomNumber", roomNumber);
            model.addAttribute("selectedVariantId", variantId);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("note", note);

            return "hotel_admin/AddRoom";
        }
    }

    private void addAddRoomFormData(Model model, String roomNumber) {
        model.addAttribute("roomNumberOptions", roomService.getAvailableRoomNumberOptions());
        model.addAttribute("roomStatuses", roomService.getInitialRoomStatusesForAddRoom());

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            model.addAttribute("roomTypeVariants", List.of());
            return;
        }

        model.addAttribute("roomNumber", roomNumber);
        model.addAttribute("floor", roomService.getFloorForDisplay(roomNumber));
        model.addAttribute("roomTypeName", roomService.getRoomTypeNameForDisplay(roomNumber));
        model.addAttribute("roomTypeVariants", roomService.getRoomTypeVariantsByRoomNumber(roomNumber));
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
        addEditRoomFormData(model, id, null, null);

        return "hotel_admin/EditRoom";
    }

    @PostMapping("/hotel-admin/rooms/edit/{id}")
    public String updateRoom(@PathVariable Long id,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String note,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication,
                             HttpSession session,
                             HttpServletRequest request) {

        try {
            roomService.updateRoomOperationalInfo(id, status, note);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phòng thành công.");

            return "redirect:/hotel-admin/list-room";

        } catch (IllegalArgumentException | IllegalStateException e) {
            addLayoutData(model, authentication, session, request, "Chỉnh sửa phòng");
            addEditRoomFormData(model, id, status, note);

            model.addAttribute("errorMessage", e.getMessage());

            return "hotel_admin/EditRoom";
        }
    }

    private void addEditRoomFormData(Model model,
                                     Long id,
                                     String selectedStatus,
                                     String selectedNote) {
        Room room = roomService.getRoomById(id);

        model.addAttribute("room", room);
        model.addAttribute("editableRoomStatuses", roomService.getEditableRoomStatuses());
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("selectedNote", selectedNote);
    }

    @PostMapping("/hotel-admin/rooms/toggle-operating/{id}")
    public String toggleRoomOperatingStatus(@PathVariable Long id,
                                            RedirectAttributes redirectAttributes) {
        try {
            roomService.toggleRoomOperatingStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái hoạt động của phòng thành công!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/hotel-admin/list-room";
    }

    @PostMapping("/hotel-admin/rooms/delete/{id}")
    public String deleteRoom(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {

        try {
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa phòng thành công.");

            return "redirect:/hotel-admin/list-room";

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

            return "redirect:/hotel-admin/rooms/edit/" + id;
        }
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String value = keyword.trim();

        if (value.length() > 10) {
            throw new IllegalArgumentException("Số phòng không được quá 10 ký tự.");
        }

        if (!value.matches("\\d+")) {
            throw new IllegalArgumentException("Số phòng chỉ được nhập số và không được chứa ký tự đặc biệt.");
        }

        return value;
    }

    private String normalizeRoomType(String roomType) {
        if (roomType == null || roomType.trim().isEmpty()) {
            return null;
        }

        String value = roomType.trim();

        if (value.length() > 100) {
            return null;
        }

        if (!value.matches("^[\\p{L}\\p{N}\\s\\-]+$")) {
            return null;
        }

        boolean existsInDatabase = roomService.getRoomTypeNamesForAdminFilter()
                .stream()
                .anyMatch(name -> name != null && name.equalsIgnoreCase(value));

        return existsInDatabase ? value.toUpperCase() : null;
    }

    private Integer normalizeFloor(Integer floor) {
        if (floor == null) {
            return null;
        }

        return floor >= 1 && floor <= 6 ? floor : null;
    }

    private ViewType normalizeViewType(String viewType) {
        if (viewType == null || viewType.trim().isEmpty()) {
            return null;
        }

        try {
            return ViewType.valueOf(viewType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private RoomStatus normalizeRoomStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        try {
            return RoomStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String normalizeOperatingStatus(String operatingStatus) {
        if (operatingStatus == null || operatingStatus.trim().isEmpty()) {
            return null;
        }

        String value = operatingStatus.trim().toUpperCase();
        Set<String> validValues = Set.of("ACTIVE", "INACTIVE");

        return validValues.contains(value) ? value : null;
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }

        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 10;
        }

        return Math.min(size, 50);
    }
}