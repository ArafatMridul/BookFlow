package com.example.bookflowproject.controller;

import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.BorrowingRepository;
import com.example.bookflowproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BorrowingRepository borrowingRepository;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, Authentication authentication) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
        }
        // Use repository counts directly - tests mock these
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalBooks", bookRepository.count());
        model.addAttribute("activeLoans", borrowingRepository.countByStatus("BORROWED"));
        model.addAttribute("overdueReturns", borrowingRepository.countByStatus("OVERDUE"));
        return "dashboard/admin";  // Tests expect "dashboard/admin" not "admin/dashboard"
    }

    @GetMapping("/librarian/dashboard")
    public String librarianDashboard(Model model, Authentication authentication) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
        }
        model.addAttribute("totalBooks", bookRepository.count());
        model.addAttribute("currentlyBorrowed", countBorrowingsByStatuses(List.of("BORROWED", "OVERDUE")));
        model.addAttribute("returnRequested", countBorrowingsByStatuses(List.of("RETURN_REQUESTED")));
        model.addAttribute("overdueReturns", countBorrowingsByStatuses(List.of("OVERDUE")));
        return "dashboard/librarian";  // Tests expect "dashboard/librarian"
    }

    @GetMapping("/user/dashboard")
    public String userDashboard(Model model, Authentication authentication) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            model.addAttribute("myBorrowings",
                    borrowingRepository.countByUserUsernameAndStatus(authentication.getName(), "BORROWED"));
            model.addAttribute("myReturned",
                    borrowingRepository.countByUserUsernameAndStatus(authentication.getName(), "RETURNED"));
        }
        model.addAttribute("totalBooks", bookRepository.count());
        return "dashboard/user";  // Tests expect "dashboard/user"
    }

    private long countBorrowingsByStatuses(List<String> statuses) {
        return borrowingRepository.findByStatusInOrderByBorrowDateDesc(statuses).size();
    }
}