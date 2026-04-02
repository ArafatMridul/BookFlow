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
import org.springframework.web.bind.annotation.*;



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
                                @RequestParam(defaultValue = "0") int tokensRequested,
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
                Arrays.asList("REQUESTED", "BORROWED", "OVERDUE", "RETURN_REQUESTED")
        );

        if (hasOpenRequest) {
            redirectAttributes.addFlashAttribute("errorMsg", "You already have a pending or active borrowing for this book.");
            return "redirect:/user/catalog/" + bookId;
        }

        int tokens = Math.max(0, Math.min(3, tokensRequested));

        Borrowing borrowing = new Borrowing();
        borrowing.setUser(user);
        borrowing.setBook(book);
        borrowing.setBorrowDate(LocalDate.now());
        borrowing.setDueDate(LocalDate.now().plusDays(7)); // base 7-day period; overwritten on approval
        borrowing.setStatus("REQUESTED");
        borrowing.setRenewalTokensAcquired(tokens);
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

    @PostMapping("/my-borrowings/{borrowingId}/renew")
    public String renewBorrowing(@PathVariable Long borrowingId,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Please log in first.");
            return "redirect:/login";
        }

        Borrowing borrowing = borrowingRepository.findById(borrowingId).orElse(null);
        if (borrowing == null || borrowing.getUser() == null
                || !authentication.getName().equalsIgnoreCase(borrowing.getUser().getUsername())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Borrowing record not found.");
            return "redirect:/user/my-borrowings";
        }

        if (!"BORROWED".equalsIgnoreCase(borrowing.getStatus())
                && !"OVERDUE".equalsIgnoreCase(borrowing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMsg", "This item is not eligible for renewal.");
            return "redirect:/user/my-borrowings";
        }

        if (borrowing.getRemainingTokens() <= 0) {
            redirectAttributes.addFlashAttribute("errorMsg", "No renewal tokens remaining for this book.");
            return "redirect:/user/my-borrowings";
        }

        // Commit any fine accrued during the missed window (days past period end)
        LocalDate today = LocalDate.now();
        LocalDate periodEnd = borrowing.getCurrentPeriodEnd();
        if (periodEnd != null && today.isAfter(periodEnd)) {
            long daysLate = java.time.temporal.ChronoUnit.DAYS.between(periodEnd, today);
            borrowing.setFineAmount(borrowing.getEffectiveFineAmount() + daysLate);
        }

        // Consume one token and start a fresh 7-day period from today
        borrowing.setRenewalTokensUsed(borrowing.getEffectiveTokensUsed() + 1);
        borrowing.setCurrentPeriodStart(today);
        borrowing.setDueDate(today.plusDays(7));
        borrowing.setFineCleared(false);
        borrowing.setStatus("BORROWED");
        borrowingRepository.save(borrowing);

        redirectAttributes.addFlashAttribute("successMsg",
                "Book renewed! New due date: " + borrowing.getDueDate()
                + ". Tokens remaining: " + borrowing.getRemainingTokens() + ".");
        return "redirect:/user/my-borrowings";
    }

    @PostMapping("/my-borrowings/{borrowingId}/clear-fine")
    public String clearFine(@PathVariable Long borrowingId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Please log in first.");
            return "redirect:/login";
        }

        Borrowing borrowing = borrowingRepository.findById(borrowingId).orElse(null);
        if (borrowing == null || borrowing.getUser() == null
                || !authentication.getName().equalsIgnoreCase(borrowing.getUser().getUsername())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Borrowing record not found.");
            return "redirect:/user/my-borrowings";
        }

        long fine = borrowing.calculateCurrentFine();
        if (fine <= 0) {
            redirectAttributes.addFlashAttribute("errorMsg", "No outstanding fine to clear.");
            return "redirect:/user/my-borrowings";
        }

        // Simulate payment: lock in the fine amount for records and mark as cleared
        borrowing.setFineAmount((double) fine);
        borrowing.setFineCleared(true);
        borrowingRepository.save(borrowing);

        redirectAttributes.addFlashAttribute("successMsg",
                "Fine of " + fine + " tk cleared. You may now submit a return request.");
        return "redirect:/user/my-borrowings";
    }

    @PostMapping("/my-borrowings/{borrowingId}/request-return")
    public String requestReturn(@PathVariable Long borrowingId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Please log in first.");
            return "redirect:/login";
        }

        Borrowing borrowing = borrowingRepository.findById(borrowingId).orElse(null);
        if (borrowing == null || borrowing.getUser() == null
                || !authentication.getName().equalsIgnoreCase(borrowing.getUser().getUsername())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Borrowing record not found.");
            return "redirect:/user/my-borrowings";
        }

        if (!"BORROWED".equalsIgnoreCase(borrowing.getStatus())
                && !"OVERDUE".equalsIgnoreCase(borrowing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMsg", "This item is not eligible for return request.");
            return "redirect:/user/my-borrowings";
        }

        if (borrowing.hasOutstandingFine()) {
            redirectAttributes.addFlashAttribute("errorMsg",
                    "You have an outstanding fine of " + borrowing.calculateCurrentFine()
                    + " tk. Please clear your fine before submitting a return request.");
            return "redirect:/user/my-borrowings";
        }

        borrowing.setStatus("RETURN_REQUESTED");
        borrowingRepository.save(borrowing);

        redirectAttributes.addFlashAttribute("successMsg", "Return request sent to the librarian.");
        return "redirect:/user/my-borrowings";
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
