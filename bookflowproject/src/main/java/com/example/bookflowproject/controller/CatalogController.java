package com.example.bookflowproject.controller;

import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.Set;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class CatalogController {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @GetMapping("/catalog")
    public String browseCatalog(@RequestParam(required = false) String search,
                                Model model, Authentication authentication) {
        var books = (search != null && !search.isBlank())
                ? bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(search, search)
                : bookRepository.findAll();

        Set<Long> wishlistIds = new HashSet<>();
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            userRepository.findByUsername(authentication.getName()).ifPresent(user ->
                    user.getWishlistBooks().forEach(b -> wishlistIds.add(b.getId())));
        }

        model.addAttribute("books", books);
        model.addAttribute("wishlistIds", wishlistIds);
        model.addAttribute("search", search != null ? search : "");
        return "dashboard/catalog";
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
