package com.example.bookflowproject.controller;

import java.time.LocalDate;
import java.util.Arrays;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bookflowproject.entity.Book;
import com.example.bookflowproject.entity.Borrowing;
import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.BorrowingRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/librarian")
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
@RequiredArgsConstructor
public class LibrarianController {

    private final BorrowingRepository borrowingRepository;
    private final BookRepository bookRepository;

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
        borrowing.setDueDate(borrowDate.plusDays(14));
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

        LocalDate dueDate = borrowing.getDueDate();
        if (dueDate != null && LocalDate.now().isAfter(dueDate)) {
            borrowing.setStatus("OVERDUE");
        } else {
            borrowing.setStatus("BORROWED");
        }

        borrowingRepository.save(borrowing);
        redirectAttributes.addFlashAttribute("successMsg", "Return request rejected.");
        return "redirect:/librarian/borrowings";
    }
}