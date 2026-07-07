package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ServiceCategoryType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PromotionRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.HomeServiceProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.PromotionProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.RoomTypeProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/page")
public class HomeController{
    private final RoomTypeRepository roomTypeRepository;
    private final ServiceRepository serviceRepository;
    private final PromotionRepository promotionRepository;
    @GetMapping("/home")
    public String home(Model model) {

        List<RoomTypeProjection> roomTypes = roomTypeRepository.findHomeRoomTypes();

        List<HomeServiceProjection> diningServices =
                serviceRepository.findServiceByCategoryType(
                        ServiceCategoryType.FOOD,
                        ImageEntityType.SERVICE,
                        PageRequest.of(0, 3)
                );

        List<HomeServiceProjection> wellnessServices =
                serviceRepository.findServiceByCategoryType(
                        ServiceCategoryType.SPA,
                        ImageEntityType.SERVICE,
                        PageRequest.of(0, 3)
                );
        PromotionProjection featuredPromotion =
                promotionRepository.findTopHomepagePromotion().orElse(null);

        model.addAttribute("featuredPromotion", featuredPromotion);
        String heroVideoUrl = "https://res.cloudinary.com/dhwtycrov/video/upload/v1781691399/homepage_kpuwig.mp4";

        model.addAttribute("heroVideoUrl", heroVideoUrl);
        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("diningServices", diningServices);
        model.addAttribute("wellnessServices", wellnessServices);

        return "page/HomePage";
    }
    @GetMapping("/overview")
    public String overview(){
        return "/page/overview";
    }
}