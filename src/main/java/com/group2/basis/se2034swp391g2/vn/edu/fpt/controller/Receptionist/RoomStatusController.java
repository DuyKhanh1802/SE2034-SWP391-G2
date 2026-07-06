package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Receptionist;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomStatusBoardResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RoomStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/receptionist/rooms")
public class RoomStatusController {

    private final RoomStatusService roomStatusService;

    @GetMapping
    public String listRoomStatus(@RequestParam(required = false) Integer floor,
                                 @RequestParam(required = false) String roomTypeName,
                                 @RequestParam(required = false) RoomStatus status,
                                 @RequestParam(required = false) String keyword,
                                 Model model) {

        List<RoomStatusBoardResponse> rooms =
                roomStatusService.getRoomStatusBoard(floor, roomTypeName, status, keyword);

        Map<Integer, List<RoomStatusBoardResponse>> roomsByFloor =
                rooms.stream()
                        .collect(Collectors.groupingBy(
                                RoomStatusBoardResponse::getFloor,
                                TreeMap::new,
                                Collectors.toList()
                        ));

        model.addAttribute("roomsByFloor", roomsByFloor);
        model.addAttribute("floors", roomStatusService.getFloors());

        model.addAttribute("roomStatuses", RoomStatus.values());

        model.addAttribute("selectedFloor", floor);
        model.addAttribute("selectedRoomTypeName", roomTypeName);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Bảng trạng thái phòng");
        return "receptionist/ListRoomStatus";
    }

    @PostMapping("/{roomId}/status")
    public String updateRoomStatus(@PathVariable Long roomId,
                                   @RequestParam RoomStatus newStatus,
                                   @RequestParam(required = false) String note,
                                   RedirectAttributes redirectAttributes) {
        try {
            roomStatusService.updateRoomStatus(roomId, newStatus, note);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái phòng thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/receptionist/rooms";
    }
}