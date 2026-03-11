package com.example.bookflowproject.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        Authentication currentAuth = authentication;
        if (currentAuth == null) {
            currentAuth = SecurityContextHolder.getContext().getAuthentication();
        }

        if (currentAuth == null || !currentAuth.isAuthenticated() || currentAuth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        // Check roles exactly as tests expect
        for (GrantedAuthority authority : currentAuth.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard";
            } else if (role.equals("ROLE_LIBRARIAN")) {
                return "redirect:/librarian/dashboard";
            } else if (role.equals("ROLE_USER")) {
                return "redirect:/user/dashboard";
            }
        }

        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }
}