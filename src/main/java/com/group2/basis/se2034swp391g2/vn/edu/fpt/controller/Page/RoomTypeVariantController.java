package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeVariantRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/page/rooms")
@Controller
public class RoomTypeVariantController {

    private final RoomTypeVariantRepository roomTypeVariantRepository;
    private final RoomTypeRepository roomTypeRepository;
    @GetMapping
    public String listRoomTypeVariant(
            @RequestParam(name = "roomTypeId", required = false) Long roomTypeId,
            @RequestParam(name = "viewType", required = false) String viewtype,
            @RequestParam(name = "sort", defaultValue = "recommended") String sort,
            @RequestParam(name = "checkInDate",required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkInDate,
            @RequestParam(name = "checkOutDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOutDate,
            @RequestParam(name = "adults",required = false, defaultValue = "1") Integer adults,
            @RequestParam(name = "children",required = false,defaultValue = "0") Integer children,
            @RequestParam(name = "roomCount", required = false,defaultValue = "1") Integer roomCount,
            @RequestParam(name = "roomGuests", required = false) String roomGuests,
            Model model
    ){
      List<RoomType> roomTypes = roomTypeRepository.findByIsDeletedFalse();

      List<GuestRoomVariantProjection> roomTypeVariant = roomTypeVariantRepository.findGuestRoomVariants(roomTypeId,viewtype,sort,checkInDate,checkOutDate,adults,children,roomCount);
      model.addAttribute("roomTypes",roomTypes);
      model.addAttribute("selectedRoomTypeId",roomTypeId);
      model.addAttribute("selectedViewType", viewtype);
      model.addAttribute("selectedSort",sort);

      model.addAttribute("checkInDate",checkInDate);
      model.addAttribute("checkOutDate",checkOutDate);
      model.addAttribute("adults",adults);
      model.addAttribute("children",children);
      model.addAttribute("roomCount",roomCount);
        model.addAttribute("roomGuests", roomGuests);

      model.addAttribute("roomOptions", roomTypeVariant);

      return "page/RoomtypeVariant";
    }
}
