package com.example.bookflowproject.controller;

import com.example.bookflowproject.entity.Book;
import com.example.bookflowproject.entity.Borrowing;
import com.example.bookflowproject.entity.Role;
import com.example.bookflowproject.entity.User;
import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.BorrowingRepository;
import com.example.bookflowproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BorrowingRepository borrowingRepository;

    @GetMapping("/users")
    public String manageUsers(@RequestParam(required = false) String search,
                              Model model,
                              Authentication authentication) {
        setUsername(model, authentication);

        List<User> users = userRepository.findAll().stream()
                .filter(user -> matchesSearch(user, search))
                .collect(Collectors.toList());

        model.addAttribute("users", users);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("adminCount", countUsersWithRole(users, "ADMIN"));
        model.addAttribute("librarianCount", countUsersWithRole(users, "LIBRARIAN"));
        model.addAttribute("memberCount", countUsersWithRole(users, "USER"));
        model.addAttribute("disabledCount", users.stream().filter(user -> !user.isEnabled()).count());
        return "admin/users";
    }

    @GetMapping("/reports")
    public String reports(Model model, Authentication authentication) {
        setUsername(model, authentication);

        List<Borrowing> recentBorrowings = borrowingRepository.findByStatusInOrderByBorrowDateDesc(
                List.of("REQUESTED", "RETURN_REQUESTED", "BORROWED", "OVERDUE", "RETURNED", "REJECTED"));

        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalBooks", bookRepository.count());
        model.addAttribute("totalLibrarians", countAllUsersWithRole("LIBRARIAN"));
        model.addAttribute("totalMembers", countAllUsersWithRole("USER"));
        model.addAttribute("pendingBorrowRequests", borrowingRepository.countByStatus("REQUESTED"));
        model.addAttribute("pendingReturnRequests", borrowingRepository.countByStatus("RETURN_REQUESTED"));
        model.addAttribute("activeLoans", borrowingRepository.countByStatus("BORROWED"));
        model.addAttribute("overdueLoans", borrowingRepository.countByStatus("OVERDUE"));
        model.addAttribute("returnedLoans", borrowingRepository.countByStatus("RETURNED"));
        model.addAttribute("rejectedRequests", borrowingRepository.countByStatus("REJECTED"));
        model.addAttribute("recentBorrowings", recentBorrowings.size() > 10 ? recentBorrowings.subList(0, 10) : recentBorrowings);
        return "admin/reports";
    }

    @GetMapping("/books")
    public String books(@RequestParam(required = false) String search,
                        Model model,
                        Authentication authentication) {
        setUsername(model, authentication);

        List<Book> books = (search != null && !search.isBlank())
                ? bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(search, search)
                : bookRepository.findAll();

        model.addAttribute("books", books);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("lowStockBooks", books.stream()
                .filter(book -> book.getAvailableCopies() != null && book.getAvailableCopies() <= 1)
                .count());
        model.addAttribute("availableBooks", books.stream()
                .filter(book -> book.getAvailableCopies() != null && book.getAvailableCopies() > 0)
                .count());
        return "admin/books";
    }

    @PostMapping("/users/{id}/toggle-enabled")
    public String toggleUserEnabled(@PathVariable Long id,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "User not found.");
            return "redirect:/admin/users";
        }

        if (authentication != null && authentication.getName().equalsIgnoreCase(user.getUsername())) {
            redirectAttributes.addFlashAttribute("errorMsg", "You cannot disable your own admin account.");
            return "redirect:/admin/users";
        }

        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMsg",
                user.isEnabled() ? "User account enabled." : "User account disabled.");
        return "redirect:/admin/users";
    }

    private void setUsername(Model model, Authentication authentication) {
        Authentication currentAuth = authentication;
        if (currentAuth == null) {
            currentAuth = SecurityContextHolder.getContext().getAuthentication();
        }
        if (currentAuth != null) {
            model.addAttribute("username", currentAuth.getName());
        }
    }

    private boolean matchesSearch(User user, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String q = search.toLowerCase(Locale.ROOT);
        return (user.getUsername() != null && user.getUsername().toLowerCase(Locale.ROOT).contains(q))
                || (user.getEmail() != null && user.getEmail().toLowerCase(Locale.ROOT).contains(q))
                || hasRoleName(user, q);
    }

    private boolean hasRoleName(User user, String query) {
        Set<Role> roles = user.getRoles();
        return roles != null && roles.stream()
                .map(Role::getName)
                .filter(java.util.Objects::nonNull)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.contains(query));
    }

    private long countUsersWithRole(List<User> users, String roleName) {
        return users.stream().filter(user -> hasRole(user, roleName)).count();
    }

    private long countAllUsersWithRole(String roleName) {
        return userRepository.findAll().stream().filter(user -> hasRole(user, roleName)).count();
    }

    private boolean hasRole(User user, String roleName) {
        Set<Role> roles = user.getRoles();
        return roles != null && roles.stream().anyMatch(role -> roleName.equalsIgnoreCase(role.getName()));
    }
}