package com.example.bookflowproject.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {


    @GetMapping("/users")
    public String manageUsers() {
        return "admin/users";
    }

    @GetMapping("/reports")
    public String reports() {
        return "admin/reports";
    }
}