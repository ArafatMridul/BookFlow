package com.example.bookflowproject.controllers;

import com.example.bookflowproject.controller.CatalogController;
import com.example.bookflowproject.entity.Book;
import com.example.bookflowproject.entity.Borrowing;
import com.example.bookflowproject.entity.User;
import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.BorrowingRepository;
import com.example.bookflowproject.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BorrowingRepository borrowingRepository;

    @InjectMocks
    private CatalogController catalogController;

    @Nested
    @DisplayName("browseCatalog")
    class BrowseCatalog {

        @Test
        void shouldPopulateCatalogWithWishlistAndBorrowingFlags() {
            Book requestedBook = book(1L, "Clean Code");
            Book borrowedBook = book(2L, "1984");
            User user = user("reader", requestedBook);

            Borrowing requested = borrowing(requestedBook, "REQUESTED");
            Borrowing borrowed = borrowing(borrowedBook, "BORROWED");

            when(bookRepository.findAll()).thenReturn(List.of(requestedBook, borrowedBook));
            when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));
            when(borrowingRepository.findByUserUsernameOrderByBorrowDateDesc("reader"))
                    .thenReturn(List.of(requested, borrowed));

            Model model = new ExtendedModelMap();
            String view = catalogController.browseCatalog(null, model, authentication("reader", "USER"));

            assertEquals("dashboard/catalog", view);
            assertEquals("reader", model.getAttribute("username"));
            assertEquals(List.of(requestedBook, borrowedBook), model.getAttribute("books"));
            assertTrue(((Set<?>) model.getAttribute("wishlistIds")).contains(1L));
            assertTrue(((Set<?>) model.getAttribute("requestedBookIds")).contains(1L));
            assertTrue(((Set<?>) model.getAttribute("borrowedBookIds")).contains(2L));
        }

        @Test
        void shouldUseSearchQueryWhenProvided() {
            when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("code", "code"))
                    .thenReturn(List.of(book(1L, "Clean Code")));
            when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user("reader")));
            when(borrowingRepository.findByUserUsernameOrderByBorrowDateDesc("reader")).thenReturn(List.of());

            Model model = new ExtendedModelMap();
            String view = catalogController.browseCatalog("code", model, authentication("reader", "USER"));

            assertEquals("dashboard/catalog", view);
            assertEquals("code", model.getAttribute("search"));
        }
    }

    @Nested
    @DisplayName("bookDetails")
    class BookDetails {

        @Test
        void shouldReturnBookDetailsPage() {
            Book book = book(10L, "Pragmatic Programmer");
            User user = user("reader");
            user.setId(7L);

            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));
            when(borrowingRepository.existsByUserIdAndBookIdAndStatusIn(eq(7L), eq(10L), anyCollection())).thenReturn(false);

            Model model = new ExtendedModelMap();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = catalogController.bookDetails(10L, model, authentication("reader", "USER"), redirectAttributes);

            assertEquals("dashboard/book-details", view);
            assertEquals(book, model.getAttribute("book"));
            assertEquals(false, model.getAttribute("alreadyRequested"));
            assertEquals(false, model.getAttribute("activeBorrowing"));
        }

        @Test
        void shouldRedirectToCatalogWhenBookMissing() {
            when(bookRepository.findById(99L)).thenReturn(Optional.empty());

            Model model = new ExtendedModelMap();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = catalogController.bookDetails(99L, model, authentication("reader", "USER"), redirectAttributes);

            assertEquals("redirect:/user/catalog", view);
            assertEquals("Book not found.", redirectAttributes.getFlashAttributes().get("errorMsg"));
        }
    }

    @Nested
    @DisplayName("requestBorrow")
    class RequestBorrow {

        @Test
        void shouldRedirectToLoginWhenBorrowRequestIsAnonymous() {
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            String view = catalogController.requestBorrow(1L, 0, null, redirectAttributes);

            assertEquals("redirect:/login", view);
            assertEquals("Please log in first.", redirectAttributes.getFlashAttributes().get("errorMsg"));
            verify(borrowingRepository, never()).save(any(Borrowing.class));
        }

        @Test
        void shouldRejectBorrowRequestWhenUserOrBookMissing() {
            when(userRepository.findByUsername("reader")).thenReturn(Optional.empty());
            when(bookRepository.findById(1L)).thenReturn(Optional.empty());

            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = catalogController.requestBorrow(1L, 0, authentication("reader", "USER"), redirectAttributes);

            assertEquals("redirect:/user/catalog", view);
            assertEquals("Unable to process request for this book.", redirectAttributes.getFlashAttributes().get("errorMsg"));
            verify(borrowingRepository, never()).save(any(Borrowing.class));
        }

        @Test
        void shouldCreateBorrowRequest() {
            User user = user("reader");
            user.setId(5L);
            Book book = book(1L, "Domain-Driven Design");

            when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(borrowingRepository.existsByUserIdAndBookIdAndStatusIn(eq(5L), eq(1L), anyCollection())).thenReturn(false);

            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = catalogController.requestBorrow(1L, 0, authentication("reader", "USER"), redirectAttributes);

            assertEquals("redirect:/user/catalog/1", view);
            assertEquals("Borrow request sent to the librarian.", redirectAttributes.getFlashAttributes().get("successMsg"));
            verify(borrowingRepository).save(any(Borrowing.class));
        }

        @Test
        void shouldRejectDuplicateRequest() {
            User user = user("reader");
            user.setId(5L);
            Book book = book(1L, "Domain-Driven Design");

            when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(borrowingRepository.existsByUserIdAndBookIdAndStatusIn(eq(5L), eq(1L), anyCollection())).thenReturn(true);

            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = catalogController.requestBorrow(1L, 0, authentication("reader", "USER"), redirectAttributes);

            assertEquals("redirect:/user/catalog/1", view);
            assertEquals("You already have a pending or active borrowing for this book.", redirectAttributes.getFlashAttributes().get("errorMsg"));
            verify(borrowingRepository, never()).save(any(Borrowing.class));
        }
    }

    @Nested
    @DisplayName("wishlist and myBorrowings")
    class WishlistAndBorrowings {

        @Test
        void shouldAddBookToWishlist() {
            User user = user("reader");
            Book book = book(3L, "Refactoring");

            when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));
            when(bookRepository.findById(3L)).thenReturn(Optional.of(book));

            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = catalogController.addToWishlist(3L, authentication("reader", "USER"), redirectAttributes);

            assertEquals("redirect:/user/catalog", view);
            assertEquals("Book added to your wishlist!", redirectAttributes.getFlashAttributes().get("successMsg"));
            assertTrue(user.getWishlistBooks().contains(book));
            verify(userRepository).save(user);
        }

        @Test
        void shouldReturnMyBorrowingsPage() {
            when(borrowingRepository.findByUserUsernameOrderByBorrowDateDesc("reader"))
                    .thenReturn(List.of(borrowing(book(8L, "Patterns"), "REQUESTED")));

            Model model = new ExtendedModelMap();
            String view = catalogController.myBorrowings(model, authentication("reader", "USER"));

            assertEquals("dashboard/my-borrowings", view);
            assertEquals("reader", model.getAttribute("username"));
            assertFalse(((List<?>) model.getAttribute("borrowings")).isEmpty());
        }

        @Test
        void shouldReturnWishlistPageWithWishlistBooks() {
            Book wishlistBook = book(12L, "Working Effectively with Legacy Code");
            User user = user("reader", wishlistBook);
            when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));

            Model model = new ExtendedModelMap();
            String view = catalogController.viewWishlist(model, authentication("reader", "USER"));

            assertEquals("dashboard/wishlist", view);
            assertEquals("reader", model.getAttribute("username"));
            assertEquals(user.getWishlistBooks(), model.getAttribute("wishlistBooks"));
        }

        @Test
        void shouldRemoveBookFromWishlistAndRespectRedirectSource() {
            Book wishlistBook = book(13L, "Test-Driven Development");
            User user = user("reader", wishlistBook);
            when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));

            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = catalogController.removeFromWishlist(13L, authentication("reader", "USER"), "wishlist", redirectAttributes);

            assertEquals("redirect:/user/wishlist", view);
            assertEquals("Book removed from your wishlist.", redirectAttributes.getFlashAttributes().get("successMsg"));
            assertFalse(user.getWishlistBooks().contains(wishlistBook));
            verify(userRepository).save(user);
        }

        @Test
        void shouldRequestReturnForBorrowedItem() {
            User user = user("reader");
            Borrowing borrowing = borrowing(book(9L, "Refactoring"), "BORROWED");
            borrowing.setUser(user);

            when(borrowingRepository.findById(15L)).thenReturn(Optional.of(borrowing));

            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = catalogController.requestReturn(15L, authentication("reader", "USER"), redirectAttributes);

            assertEquals("redirect:/user/my-borrowings", view);
            assertEquals("Return request sent to the librarian.", redirectAttributes.getFlashAttributes().get("successMsg"));
            verify(borrowingRepository).save(any(Borrowing.class));
        }

        @Test
        void shouldRejectReturnWhenBorrowingBelongsToAnotherUser() {
            User otherUser = user("another-reader");
            Borrowing borrowing = borrowing(book(10L, "Design Patterns"), "BORROWED");
            borrowing.setUser(otherUser);

            when(borrowingRepository.findById(16L)).thenReturn(Optional.of(borrowing));

            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = catalogController.requestReturn(16L, authentication("reader", "USER"), redirectAttributes);

            assertEquals("redirect:/user/my-borrowings", view);
            assertEquals("Borrowing record not found.", redirectAttributes.getFlashAttributes().get("errorMsg"));
            verify(borrowingRepository, never()).save(any(Borrowing.class));
        }

        @Test
        void shouldRejectReturnWhenStatusIsNotEligible() {
            User user = user("reader");
            Borrowing borrowing = borrowing(book(11L, "Clean Architecture"), "REQUESTED");
            borrowing.setUser(user);

            when(borrowingRepository.findById(17L)).thenReturn(Optional.of(borrowing));

            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = catalogController.requestReturn(17L, authentication("reader", "USER"), redirectAttributes);

            assertEquals("redirect:/user/my-borrowings", view);
            assertEquals("This item is not eligible for return request.", redirectAttributes.getFlashAttributes().get("errorMsg"));
            verify(borrowingRepository, never()).save(any(Borrowing.class));
        }

        @Test
        void shouldRedirectToLoginWhenRequestReturnIsAnonymous() {
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            String view = catalogController.requestReturn(18L, null, redirectAttributes);

            assertEquals("redirect:/login", view);
            assertEquals("Please log in first.", redirectAttributes.getFlashAttributes().get("errorMsg"));
            verify(borrowingRepository, never()).findById(any(Long.class));
            verify(borrowingRepository, never()).save(any(Borrowing.class));
        }
    }

    private UsernamePasswordAuthenticationToken authentication(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    private Book book(Long id, String title) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor("Author");
        book.setAvailableCopies(3);
        book.setTotalCopies(5);
        return book;
    }

    private User user(String username, Book... wishlistBooks) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setWishlistBooks(Arrays.stream(wishlistBooks).collect(java.util.stream.Collectors.toSet()));
        return user;
    }

    private Borrowing borrowing(Book book, String status) {
        Borrowing borrowing = new Borrowing();
        borrowing.setBook(book);
        borrowing.setStatus(status);
        borrowing.setBorrowDate(LocalDate.now());
        borrowing.setDueDate(LocalDate.now().plusDays(14));
        return borrowing;
    }
}
