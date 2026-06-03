package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Guest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String index(Model model) {
        // 2. Dữ liệu Recommendation Service mẫu
        List<ServiceDto> recs = new ArrayList<>();
        recs.add(new ServiceDto("Art & Drinks", "https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?q=80&w=600", "Handcrafted cocktails."));
        recs.add(new ServiceDto("Fine Pastry", "https://images.unsplash.com/photo-1608897013039-887f21d8c804?q=80&w=600", "Delightful afternoon tea experience."));
        recs.add(new ServiceDto("Perfume Lab", "https://images.unsplash.com/photo-1547887537-6158d64c35b3?q=80&w=600", "Bespoke fragrance curation."));
        model.addAttribute("recommendedServices", recs);

        // 3. Dữ liệu Room Category mẫu cho Slider lớn
        List<CategoryDto> rooms = new ArrayList<>();
        rooms.add(new CategoryDto(1L, "Harbour View Room", "https://images.unsplash.com/photo-1618773928121-c32242e63f39?q=80&w=1200"));
        rooms.add(new CategoryDto(2L, "Grand Suite", "https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1200"));
        model.addAttribute("roomCategories", rooms);

        // 4. Dữ liệu Nhà hàng (Dining) mẫu
        List<RestaurantDto> dining = new ArrayList<>();
        dining.add(new RestaurantDto(1L, "Marmo Bistro", "Inspired by the timeless elegance of marble, classics reimagined.", "https://images.unsplash.com/photo-1550966871-3ed3cdb5ed0c?q=80&w=600"));
        dining.add(new RestaurantDto(2L, "The Legacy House", "MICHELIN Guide Hong Kong & Macau One-Star Cantonese cuisine.", "https://images.unsplash.com/photo-1552566626-52f8b828add9?q=80&w=600"));
        dining.add(new RestaurantDto(3L, "CHAAT", "Award-winning and refined take on India's dynamic street food.", "https://images.unsplash.com/photo-1585938338392-50a59970d2ee?q=80&w=600"));
        model.addAttribute("diningServices", dining);

        return "page/HomePage";
    }
}

// Định nghĩa nhanh các DTO class để nhận dữ liệu (Bạn thay thế bằng Entity thật của bạn)
class ServiceDto {
    public String name; public String imageUrl; public String shortDescription;
    public ServiceDto(String n, String i, String s) { this.name = n; this.imageUrl = i; this.shortDescription = s; }
}
class CategoryDto {
    public Long id; public String name; public String imageUrl;
    public CategoryDto(Long id, String n, String i) { this.id = id; this.name = n; this.imageUrl = i; }
}
class RestaurantDto {
    public Long id; public String name; public String description; public String imageUrl;
    public RestaurantDto(Long id, String n, String d, String i) { this.id = id; this.name = n; this.description = d; this.imageUrl = i; }
}