package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.HomeRoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.Banner;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class RoomTypeController {
    private final RoomTypeRepository roomTypeRepository;
    @GetMapping("room-types")
    public String roomTypes(Model model){
        List<HomeRoomType> roomTypes = roomTypeRepository.findAllRoomtypeForHome(ImageEntityType.ROOM_TYPE);
        model.addAttribute("roomTypes",roomTypes);
        return "page/RoomTypePage";
    }
}
