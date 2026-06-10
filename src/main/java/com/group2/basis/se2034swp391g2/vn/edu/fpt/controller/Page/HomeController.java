package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page;


import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ServiceCategoryType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.HomeRoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.HomeService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import lombok.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@Controller
@RequestMapping("/home")
public class HomeController {

    private final RoomTypeRepository roomTypeRepository;
    private final ServiceRepository serviceRepository;

    @GetMapping()
    public String home(Model model){
        List<HomeRoomType> roomTypes = roomTypeRepository.findAllRoomtypeForHome(ImageEntityType.ROOM_TYPE);

        List<HomeService> featuredService = serviceRepository.findServiceForHome(ImageEntityType.SERVICE, PageRequest.of(0,3));

        List<HomeService> diningService = serviceRepository.findServiceByCategoryType(ServiceCategoryType.FOOD,
                                                                                      ImageEntityType.SERVICE,
                                                                                      PageRequest.of(0,3));

        List<HomeService> wellnessService = serviceRepository.findServiceByCategoryType(ServiceCategoryType.SPA,ImageEntityType.SERVICE,PageRequest.of(0,3));

        String heroVideoUrl = "https://res.cloudinary.com/dhwtycrov/video/upload/video_cluubu.mp4";

        model.addAttribute("heroVideoUrl", heroVideoUrl);
        model.addAttribute("roomTypes",roomTypes);
        model.addAttribute("featuredService",featuredService);
        model.addAttribute("diningService",diningService);
        model.addAttribute("wellnessService",wellnessService);

        return "page/HomePage";

    }

        @GetMapping("/room-types")
        public String roomTypes(Model model){
            List<HomeRoomType> roomTypes = roomTypeRepository.findAllRoomtypeForHome(ImageEntityType.ROOM_TYPE);
            model.addAttribute("roomTypes",roomTypes);
            return "page/RoomTypePage";
        }
}
