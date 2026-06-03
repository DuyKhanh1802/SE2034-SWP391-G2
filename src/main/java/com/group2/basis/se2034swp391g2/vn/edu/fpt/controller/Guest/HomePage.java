package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Guest;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomePage {

    @GetMapping({"/home", "/"}) // Thêm dấu "/" để trang chủ load mặc định
    public String index(Model model, @AuthenticationPrincipal User user) {

        // Kiểm tra xem người dùng đã đăng nhập hay chưa
        if (user != null) {
            model.addAttribute("email", user.getEmail());
            // Có thể truyền thêm các thông tin khác nếu cần
        } else {
            // Nếu chưa đăng nhập (Guest), truyền một giá trị mặc định hoặc không làm gì cả
            model.addAttribute("email", "Guest");
        }

        return "guest/HomePage";
    }
}