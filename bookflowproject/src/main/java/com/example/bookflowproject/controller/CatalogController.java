package com.example.bookflowproject.controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bookflowproject.entity.Borrowing;
import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.BorrowingRepository;
import com.example.bookflowproject.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class CatalogController {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowingRepository borrowingRepository;

    @GetMapping("/catalog")
    public String browseCatalog(@RequestParam(required = false) String search,
                                Model model, Authentication authentication) {
        var books = (search != null && !search.isBlank())
                ? bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(search, search)
                : bookRepository.findAll();

        Set<Long> wishlistIds = new HashSet<>();
        Set<Long> requestedBookIds = new HashSet<>();
        Set<Long> borrowedBookIds = new HashSet<>();
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
                user.getWishlistBooks().forEach(b -> wishlistIds.add(b.getId()));
                List<Borrowing> myBorrowings = borrowingRepository.findByUserUsernameOrderByBorrowDateDesc(authentication.getName());
                for (Borrowing borrowing : myBorrowings) {
                    if (borrowing.getBook() == null || borrowing.getBook().getId() == null) {
                        continue;
                    }
                    if ("REQUESTED".equalsIgnoreCase(borrowing.getStatus())) {
                        requestedBookIds.add(borrowing.getBook().getId());
                    }
                    if ("BORROWED".equalsIgnoreCase(borrowing.getStatus()) || "OVERDUE".equalsIgnoreCase(borrowing.getStatus())) {
                        borrowedBookIds.add(borrowing.getBook().getId());
                    }
                }
            });
        }

        model.addAttribute("books", books);
        model.addAttribute("wishlistIds", wishlistIds);
        model.addAttribute("requestedBookIds", requestedBookIds);
        model.addAttribute("borrowedBookIds", borrowedBookIds);
        model.addAttribute("search", search != null ? search : "");
        return "dashboard/catalog";
    }

    @GetMapping("/catalog/{bookId}")
    public String bookDetails(@PathVariable Long bookId, Model model, Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
        }

        var book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Book not found.");
            return "redirect:/user/catalog";
        }

        boolean alreadyRequested = false;
        boolean activeBorrowing = false;
        if (authentication != null) {
            var userOpt = userRepository.findByUsername(authentication.getName());
            if (userOpt.isPresent()) {
                Long userId = userOpt.get().getId();
                alreadyRequested = borrowingRepository.existsByUserIdAndBookIdAndStatusIn(
                        userId,
                        bookId,
                        Arrays.asList("REQUESTED")
                );
                activeBorrowing = borrowingRepository.existsByUserIdAndBookIdAndStatusIn(
                        userId,
                        bookId,
                        Arrays.asList("BORROWED", "OVERDUE")
                );
            }
        }

        model.addAttribute("book", book);
        model.addAttribute("alreadyRequested", alreadyRequested);
        model.addAttribute("activeBorrowing", activeBorrowing);
        return "dashboard/book-details";
    }

    @PostMapping("/catalog/{bookId}/request-borrow")
    public String requestBorrow(@PathVariable Long bookId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Please log in first.");
            return "redirect:/login";
        }

        var user = userRepository.findByUsername(authentication.getName()).orElse(null);
        var book = bookRepository.findById(bookId).orElse(null);

        if (user == null || book == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to process request for this book.");
            return "redirect:/user/catalog";
        }

        boolean hasOpenRequest = borrowingRepository.existsByUserIdAndBookIdAndStatusIn(
                user.getId(),
                bookId,
                Arrays.asList("REQUESTED", "BORROWED", "OVERDUE")
        );

        if (hasOpenRequest) {
            redirectAttributes.addFlashAttribute("errorMsg", "You already have a pending or active borrowing for this book.");
            return "redirect:/user/catalog/" + bookId;
        }

        Borrowing borrowing = new Borrowing();
        borrowing.setUser(user);
        borrowing.setBook(book);
        borrowing.setBorrowDate(LocalDate.now());
        borrowing.setDueDate(LocalDate.now().plusDays(14));
        borrowing.setStatus("REQUESTED");
        borrowingRepository.save(borrowing);

        redirectAttributes.addFlashAttribute("successMsg", "Borrow request sent to the librarian.");
        return "redirect:/user/catalog/" + bookId;
    }

    @GetMapping("/my-borrowings")
    public String myBorrowings(Model model, Authentication authentication) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            model.addAttribute("borrowings", borrowingRepository.findByUserUsernameOrderByBorrowDateDesc(authentication.getName()));
        }
        return "dashboard/my-borrowings";
    }

    @GetMapping("/wishlist")
    public String viewWishlist(Model model, Authentication authentication) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            userRepository.findByUsername(authentication.getName()).ifPresent(user ->
                    model.addAttribute("wishlistBooks", user.getWishlistBooks()));
        }
        return "dashboard/wishlist";
    }

    @PostMapping("/wishlist/add/{bookId}")
    public String addToWishlist(@PathVariable Long bookId, Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication != null) {
            userRepository.findByUsername(authentication.getName()).ifPresent(user ->
                    bookRepository.findById(bookId).ifPresent(book -> {
                        user.getWishlistBooks().add(book);
                        userRepository.save(user);
                    }));
            redirectAttributes.addFlashAttribute("successMsg", "Book added to your wishlist!");
        }
        return "redirect:/user/catalog";
    }

    @PostMapping("/wishlist/remove/{bookId}")
    public String removeFromWishlist(@PathVariable Long bookId, Authentication authentication,
                                     @RequestParam(defaultValue = "catalog") String from,
                                     RedirectAttributes redirectAttributes) {
        if (authentication != null) {
            userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
                user.getWishlistBooks().removeIf(b -> b.getId().equals(bookId));
                userRepository.save(user);
            });
            redirectAttributes.addFlashAttribute("successMsg", "Book removed from your wishlist.");
        }
        return "redirect:/user/" + from;
    }
}
