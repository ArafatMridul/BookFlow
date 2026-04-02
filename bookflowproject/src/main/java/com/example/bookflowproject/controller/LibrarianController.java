package com.example.bookflowproject.controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bookflowproject.entity.Book;
import com.example.bookflowproject.entity.Borrowing;
import com.example.bookflowproject.entity.User;
import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.BorrowingRepository;
import com.example.bookflowproject.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/librarian")
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
@RequiredArgsConstructor
public class LibrarianController {

    private final BorrowingRepository borrowingRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @GetMapping({"/books", "/books/manage"})
    public String manageBooks(@RequestParam(required = false) String search,
                              @RequestParam(required = false) Long bookId,
                              Model model,
                              Authentication authentication) {
        Authentication currentAuth = authentication;
        if (currentAuth == null) {
            currentAuth = SecurityContextHolder.getContext().getAuthentication();
        }
        if (currentAuth != null) {
            model.addAttribute("username", currentAuth.getName());
        }

        List<Book> books = (search != null && !search.isBlank())
                ? bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(search, search)
                : bookRepository.findAll();
        model.addAttribute("books", books);
        model.addAttribute("availableBooks", books.stream()
                .filter(book -> book.getAvailableCopies() != null && book.getAvailableCopies() > 0)
                .collect(Collectors.toList()));
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedBook", bookId != null ? bookRepository.findById(bookId).orElse(null) : null);
        return "dashboard/librarian-books";
    }

    @GetMapping("/books/add")
    public String addBookForm(Model model, Authentication authentication) {
        Authentication currentAuth = authentication;
        if (currentAuth == null) {
            currentAuth = SecurityContextHolder.getContext().getAuthentication();
        }
        if (currentAuth != null) {
            model.addAttribute("username", currentAuth.getName());
        }
        model.addAttribute("bookForm", new Book());
        return "dashboard/librarian-books-add";
    }

    @PostMapping("/books/add")
    public String addBook(@ModelAttribute("bookForm") Book book,
                          RedirectAttributes redirectAttributes) {
        if (book.getTitle() == null || book.getTitle().isBlank()
                || book.getAuthor() == null || book.getAuthor().isBlank()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Title and author are required.");
            return "redirect:/librarian/books/add";
        }

        if (book.getIsbn() != null && !book.getIsbn().isBlank() && bookRepository.existsByIsbn(book.getIsbn())) {
            redirectAttributes.addFlashAttribute("errorMsg", "A book with this ISBN already exists.");
            return "redirect:/librarian/books/add";
        }

        int totalCopies = book.getTotalCopies() != null && book.getTotalCopies() > 0 ? book.getTotalCopies() : 1;
        book.setTotalCopies(totalCopies);

        int availableCopies = book.getAvailableCopies() != null && book.getAvailableCopies() >= 0
                ? book.getAvailableCopies()
                : totalCopies;
        book.setAvailableCopies(Math.min(availableCopies, totalCopies));

        try {
            bookRepository.save(book);
            redirectAttributes.addFlashAttribute("successMsg", "Book added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to add book: " + e.getMessage());
        }
        return "redirect:/librarian/books/manage";
    }

    @PostMapping("/books/{id}/update")
    public String updateBook(@PathVariable Long id,
                             @ModelAttribute Book book,
                             RedirectAttributes redirectAttributes) {
        Book existing = bookRepository.findById(id).orElse(null);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Book not found.");
            return "redirect:/librarian/books/manage";
        }

        if (book.getTitle() == null || book.getTitle().isBlank()
                || book.getAuthor() == null || book.getAuthor().isBlank()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Title and author are required.");
            return "redirect:/librarian/books/manage?bookId=" + id;
        }

        if (book.getIsbn() != null && !book.getIsbn().isBlank()
                && bookRepository.existsByIsbnAndIdNot(book.getIsbn(), id)) {
            redirectAttributes.addFlashAttribute("errorMsg", "A book with this ISBN already exists.");
            return "redirect:/librarian/books/manage?bookId=" + id;
        }

        int totalCopies = book.getTotalCopies() != null && book.getTotalCopies() > 0 ? book.getTotalCopies() : 1;
        int availableCopies;
        if (book.getAvailableCopies() != null && book.getAvailableCopies() >= 0) {
            availableCopies = Math.min(book.getAvailableCopies(), totalCopies);
        } else if (existing.getAvailableCopies() != null) {
            availableCopies = Math.min(existing.getAvailableCopies(), totalCopies);
        } else {
            availableCopies = totalCopies;
        }

        existing.setTitle(book.getTitle());
        existing.setAuthor(book.getAuthor());
        existing.setIsbn(book.getIsbn());
        existing.setPublicationYear(book.getPublicationYear());
        existing.setPublisher(book.getPublisher());
        existing.setCoverImageUrl(book.getCoverImageUrl());
        existing.setTotalCopies(totalCopies);
        existing.setAvailableCopies(availableCopies);

        bookRepository.save(existing);
        redirectAttributes.addFlashAttribute("successMsg", "Book updated successfully.");
        return "redirect:/librarian/books/manage?bookId=" + id;
    }

    @PostMapping("/books/{id}/delete")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Book existing = bookRepository.findById(id).orElse(null);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Book not found.");
            return "redirect:/librarian/books/manage";
        }

        if (borrowingRepository.existsByBookId(id)) {
            redirectAttributes.addFlashAttribute("errorMsg", "This book cannot be deleted because borrowing records already exist.");
            return "redirect:/librarian/books/manage?bookId=" + id;
        }

        bookRepository.delete(existing);
        redirectAttributes.addFlashAttribute("successMsg", "Book deleted successfully.");
        return "redirect:/librarian/books/manage";
    }

    @GetMapping("/patrons")
    public String searchPatrons(@RequestParam(required = false) String search,
                                Model model,
                                Authentication authentication) {
        Authentication currentAuth = authentication;
        if (currentAuth == null) {
            currentAuth = SecurityContextHolder.getContext().getAuthentication();
        }
        if (currentAuth != null) {
            model.addAttribute("username", currentAuth.getName());
        }

        List<User> patrons = userRepository.findAll().stream()
                .filter(user -> search == null || search.isBlank()
                        || (user.getUsername() != null && user.getUsername().toLowerCase().contains(search.toLowerCase()))
                        || (user.getEmail() != null && user.getEmail().toLowerCase().contains(search.toLowerCase())))
                .collect(Collectors.toList());
        model.addAttribute("patrons", patrons);
        model.addAttribute("search", search != null ? search : "");
        return "dashboard/librarian-patrons";
    }

    @GetMapping("/borrowings")
    public String manageBorrowings(Model model, Authentication authentication) {
        Authentication currentAuth = authentication;
        if (currentAuth == null) {
            currentAuth = SecurityContextHolder.getContext().getAuthentication();
        }
        if (currentAuth != null) {
            model.addAttribute("username", currentAuth.getName());
        }
        model.addAttribute("requestedBorrowings", borrowingRepository.findByStatusOrderByBorrowDateDesc("REQUESTED"));
        model.addAttribute("returnRequestedBorrowings", borrowingRepository.findByStatusOrderByBorrowDateDesc("RETURN_REQUESTED"));
        model.addAttribute("activeBorrowings", borrowingRepository.findByStatusInOrderByBorrowDateDesc(Arrays.asList("BORROWED", "OVERDUE")));
        return "dashboard/librarian-borrowings";
    }

    @PostMapping("/borrowings/{id}/approve")
    public String approveRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Borrowing borrowing = borrowingRepository.findById(id).orElse(null);
        if (borrowing == null || !"REQUESTED".equalsIgnoreCase(borrowing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Borrow request not found.");
            return "redirect:/librarian/borrowings";
        }

        Book book = borrowing.getBook();
        if (book == null || book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            redirectAttributes.addFlashAttribute("errorMsg", "Book is unavailable. Cannot approve this request.");
            return "redirect:/librarian/borrowings";
        }

        LocalDate borrowDate = LocalDate.now();
        borrowing.setBorrowDate(borrowDate);
        borrowing.setCurrentPeriodStart(borrowDate);
        borrowing.setDueDate(borrowDate.plusDays(7)); // base 7-day period
        borrowing.setReturnDate(null);
        borrowing.setStatus("BORROWED");
        borrowingRepository.save(borrowing);

        Integer currentAvailableValue = book.getAvailableCopies();
        int currentAvailable = currentAvailableValue != null ? currentAvailableValue : 0;
        book.setAvailableCopies(Math.max(currentAvailable - 1, 0));
        bookRepository.save(book);
        redirectAttributes.addFlashAttribute("successMsg", "Borrow request approved.");
        return "redirect:/librarian/borrowings";
    }

    @PostMapping("/borrowings/{id}/reject")
    public String rejectRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Borrowing borrowing = borrowingRepository.findById(id).orElse(null);
        if (borrowing == null || !"REQUESTED".equalsIgnoreCase(borrowing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Borrow request not found.");
            return "redirect:/librarian/borrowings";
        }

        borrowing.setStatus("REJECTED");
        borrowingRepository.save(borrowing);
        redirectAttributes.addFlashAttribute("successMsg", "Borrow request rejected.");
        return "redirect:/librarian/borrowings";
    }

    @PostMapping("/borrowings/{id}/process-return")
    public String processReturn(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Borrowing borrowing = borrowingRepository.findById(id).orElse(null);
        if (borrowing == null || !"RETURN_REQUESTED".equalsIgnoreCase(borrowing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Return request not found.");
            return "redirect:/librarian/borrowings";
        }

        Book book = borrowing.getBook();
        if (book == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Book not found for this borrowing record.");
            return "redirect:/librarian/borrowings";
        }

        borrowing.setStatus("RETURNED");
        borrowing.setReturnDate(LocalDate.now());
        borrowingRepository.save(borrowing);

        int availableCopies = book.getAvailableCopies() != null ? book.getAvailableCopies() : 0;
        int totalCopies = book.getTotalCopies() != null ? book.getTotalCopies() : 0;
        book.setAvailableCopies(Math.min(availableCopies + 1, Math.max(totalCopies, 0)));
        bookRepository.save(book);

        redirectAttributes.addFlashAttribute("successMsg", "Return request processed successfully.");
        return "redirect:/librarian/borrowings";
    }

    @PostMapping("/borrowings/{id}/reject-return")
    public String rejectReturnRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Borrowing borrowing = borrowingRepository.findById(id).orElse(null);
        if (borrowing == null || !"RETURN_REQUESTED".equalsIgnoreCase(borrowing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Return request not found.");
            return "redirect:/librarian/borrowings";
        }

        LocalDate periodEnd = borrowing.getCurrentPeriodEnd();
        boolean pastDeadline = periodEnd != null && LocalDate.now().isAfter(periodEnd)
                && borrowing.getRemainingTokens() == 0;
        borrowing.setStatus(pastDeadline ? "OVERDUE" : "BORROWED");

        borrowingRepository.save(borrowing);
        redirectAttributes.addFlashAttribute("successMsg", "Return request rejected.");
        return "redirect:/librarian/borrowings";
    }
}