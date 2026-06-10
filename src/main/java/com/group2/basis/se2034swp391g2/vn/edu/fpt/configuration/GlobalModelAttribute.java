package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttribute {

    @ModelAttribute
    public void addCurrentUri(
            HttpServletRequest request,
            Model model) {

        model.addAttribute(
                "currentUri",
                request.getRequestURI()
        );
    }
}
