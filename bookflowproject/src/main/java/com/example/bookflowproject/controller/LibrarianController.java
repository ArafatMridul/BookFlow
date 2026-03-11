package com.example.bookflowproject.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/librarian")
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
public class LibrarianController {


    @GetMapping("/borrowings")
    public String manageBorrowings() {
        return "librarian/borrowings";
    }
}