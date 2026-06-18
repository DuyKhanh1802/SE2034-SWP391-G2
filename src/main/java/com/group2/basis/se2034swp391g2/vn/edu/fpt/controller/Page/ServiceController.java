package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.ServiceProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ServicesService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import lombok.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/services")
public class ServiceController {

    private final ServicesService servicesService;

    @GetMapping("/dining")
    public String viewDiningPage(
            @RequestParam(name = "page",defaultValue = "0") int page,
            Model model
    ){
        Page<ServiceProjection> diningPage = servicesService.findListDining(page);
        String heroVideourl = "https://res.cloudinary.com/dhwtycrov/video/upload/v1781770392/dining_hgw4y4.mp4";
        model.addAttribute("diningPage",diningPage);
        model.addAttribute("diningList",diningPage.getContent());
        model.addAttribute("currentPage",page);
        model.addAttribute("totalPages",diningPage.getTotalPages());
        model.addAttribute("heroVideoUrl",heroVideourl);


        return "page/Dining";

    }


}
