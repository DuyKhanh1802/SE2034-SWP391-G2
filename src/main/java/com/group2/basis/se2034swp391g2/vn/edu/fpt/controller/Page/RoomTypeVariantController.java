    package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page;

    import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
    import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
    import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeVariantRepository;
    import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
    import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.RoomVariantDetailProjection;
    import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RoomTypeVariantService;
    import org.springframework.format.annotation.DateTimeFormat;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestParam;
    import lombok.*;

    import java.time.LocalDate;
    import java.util.List;

    @RequiredArgsConstructor
    @RequestMapping("/page")
    @Controller
    public class RoomTypeVariantController {

        private final RoomTypeVariantService roomTypeVariantService;

        @GetMapping("/rooms")
        public String listRoomTypeVariant(
                @RequestParam(name = "roomTypeId", required = false) Long roomTypeId,

                @RequestParam(name = "viewType", required = false) String viewType,

                @RequestParam(name = "sort", defaultValue = "recommended") String sort,

                @RequestParam(name = "checkInDate", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate checkInDate,

                @RequestParam(name = "checkOutDate", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate checkOutDate,

                @RequestParam(name = "adults", required = false, defaultValue = "1") Integer adults,

                @RequestParam(name = "children", required = false, defaultValue = "0") Integer children,

                @RequestParam(name = "roomCount", required = false, defaultValue = "1") Integer roomCount,

                @RequestParam(name = "roomGuests", required = false) String roomGuests,

                Model model
        ) {
            List<RoomType> roomTypes = roomTypeVariantService.getRoomTypes();

            List<GuestRoomVariantProjection> roomTypeVariants =
                    roomTypeVariantService.getGuestRoomVariants(
                            roomTypeId,
                            viewType,
                            sort,
                            checkInDate,
                            checkOutDate,
                            adults,
                            children,
                            roomCount
                    );

            model.addAttribute("roomTypes", roomTypes);
            model.addAttribute("roomOptions", roomTypeVariants);

            model.addAttribute("selectedRoomTypeId", roomTypeId);
            model.addAttribute("selectedViewType", viewType);
            model.addAttribute("selectedSort", sort);

            model.addAttribute("checkInDate", checkInDate);
            model.addAttribute("checkOutDate", checkOutDate);
            model.addAttribute("adults", adults);
            model.addAttribute("children", children);
            model.addAttribute("roomCount", roomCount);
            model.addAttribute("roomGuests", roomGuests);

            return "page/RoomTypeVariant";
        }
        @GetMapping("room-variants/{variantId}")
      public String viewRoomTypeVariantDetail(
              @PathVariable Long variantId,
              @RequestParam(required = false)
              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
              LocalDate checkInDate,

              @RequestParam(required = false)
              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
              LocalDate checkOutDate,

              Model model
      ){
          RoomVariantDetailProjection room = roomTypeVariantService.getRoomVariantDetail(variantId,checkInDate,checkOutDate);

          model.addAttribute("room",room);
          model.addAttribute("checkInDate",checkInDate);
          model.addAttribute("checkOutDate",checkOutDate);

          return "page/RoomTypeVariantDetail";

      }


    }

