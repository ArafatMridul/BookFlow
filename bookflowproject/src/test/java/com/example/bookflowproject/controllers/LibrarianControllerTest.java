package com.example.bookflowproject.controllers;

import com.example.bookflowproject.config.JwtProperties;
import com.example.bookflowproject.controller.LibrarianController;
import com.example.bookflowproject.entity.Book;
import com.example.bookflowproject.entity.Borrowing;
import com.example.bookflowproject.entity.Role;
import com.example.bookflowproject.entity.User;
import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.BorrowingRepository;
import com.example.bookflowproject.repository.UserRepository;
import com.example.bookflowproject.security.JwtAuthenticationFilter;
import com.example.bookflowproject.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(LibrarianController.class)
@AutoConfigureMockMvc(addFilters = false)
class LibrarianControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BorrowingRepository borrowingRepository;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("GET /librarian/borrowings")
    class ManageBorrowings {

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldReturnBorrowingsDashboard() throws Exception {
            when(borrowingRepository.findByStatusOrderByBorrowDateDesc("REQUESTED"))
                    .thenReturn(List.of(borrowing(1L, "REQUESTED", 2)));
            when(borrowingRepository.findByStatusOrderByBorrowDateDesc("BORROWED"))
                    .thenReturn(List.of(borrowing(2L, "BORROWED", 1)));

            mockMvc.perform(get("/librarian/borrowings"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/librarian-borrowings"))
                    .andExpect(model().attribute("username", "librarian"))
                    .andExpect(model().attributeExists("requestedBorrowings", "activeBorrowings"));
        }
    }

    @Nested
    @DisplayName("GET /librarian/books/manage")
    class ManageBooks {

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldReturnBookManagementPage() throws Exception {
            when(bookRepository.findAll()).thenReturn(List.of(book()));

            mockMvc.perform(get("/librarian/books/manage"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/librarian-books"))
                    .andExpect(model().attribute("username", "librarian"))
                    .andExpect(model().attributeExists("books", "availableBooks"));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldLoadSelectedBookForEditing() throws Exception {
            when(bookRepository.findAll()).thenReturn(List.of(book()));
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book()));

            mockMvc.perform(get("/librarian/books/manage").param("bookId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/librarian-books"))
                    .andExpect(model().attributeExists("selectedBook"));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldUpdateExistingBook() throws Exception {
            Book existing = book();
            existing.setId(1L);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

            mockMvc.perform(post("/librarian/books/1/update")
                            .param("title", "Effective Java, 3rd Edition")
                            .param("author", "Joshua Bloch")
                            .param("isbn", "9780134685991")
                            .param("totalCopies", "4")
                            .param("availableCopies", "2"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/books/manage?bookId=1"))
                    .andExpect(flash().attribute("successMsg", "Book updated successfully."));

            verify(bookRepository).save(any(Book.class));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldDeleteBookWhenNoBorrowingRecordsExist() throws Exception {
            Book existing = book();
            existing.setId(1L);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(borrowingRepository.existsByBookId(1L)).thenReturn(false);

            mockMvc.perform(post("/librarian/books/1/delete"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/books/manage"))
                    .andExpect(flash().attribute("successMsg", "Book deleted successfully."));

            verify(bookRepository).delete(existing);
        }
    }

    @Nested
    @DisplayName("GET /librarian/books/add")
    class AddBookForm {

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldReturnAddBookForm() throws Exception {
            mockMvc.perform(get("/librarian/books/add"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/librarian-books-add"))
                    .andExpect(model().attributeExists("bookForm"));
        }
    }

    @Nested
    @DisplayName("POST /librarian/books/add")
    class AddBook {

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldSaveNewBook() throws Exception {
            mockMvc.perform(post("/librarian/books/add")
                            .param("title", "Clean Architecture")
                            .param("author", "Robert C. Martin")
                            .param("isbn", "9780134494166")
                            .param("totalCopies", "3")
                            .param("availableCopies", "3"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/books/manage"))
                    .andExpect(flash().attribute("successMsg", "Book added successfully."));

            verify(bookRepository).save(any(Book.class));
        }
    }

    @Nested
    @DisplayName("GET /librarian/patrons")
    class SearchPatrons {

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldReturnPatronSearchPage() throws Exception {
            when(userRepository.findAll()).thenReturn(List.of(user()));

            mockMvc.perform(get("/librarian/patrons"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/librarian-patrons"))
                    .andExpect(model().attribute("username", "librarian"))
                    .andExpect(model().attributeExists("patrons"));
        }
    }

    @Nested
    @DisplayName("POST /librarian/borrowings/{id}/approve")
    class ApproveRequest {

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldApproveRequestAndDecreaseBookStock() throws Exception {
            Borrowing borrowing = borrowing(10L, "REQUESTED", 3);
            when(borrowingRepository.findById(10L)).thenReturn(Optional.of(borrowing));

            mockMvc.perform(post("/librarian/borrowings/10/approve"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/borrowings"))
                    .andExpect(flash().attribute("successMsg", "Borrow request approved."));

            verify(borrowingRepository).save(any(Borrowing.class));
            verify(bookRepository).save(any(Book.class));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectApprovalWhenBookUnavailable() throws Exception {
            Borrowing borrowing = borrowing(10L, "REQUESTED", 0);
            when(borrowingRepository.findById(10L)).thenReturn(Optional.of(borrowing));

            mockMvc.perform(post("/librarian/borrowings/10/approve"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/borrowings"))
                    .andExpect(flash().attribute("errorMsg", "Book is unavailable. Cannot approve this request."));

            verify(bookRepository, never()).save(any(Book.class));
        }
    }

    @Nested
    @DisplayName("POST /librarian/borrowings/{id}/reject")
    class RejectRequest {

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectRequest() throws Exception {
            Borrowing borrowing = borrowing(11L, "REQUESTED", 2);
            when(borrowingRepository.findById(11L)).thenReturn(Optional.of(borrowing));

            mockMvc.perform(post("/librarian/borrowings/11/reject"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/borrowings"))
                    .andExpect(flash().attribute("successMsg", "Borrow request rejected."));

            verify(borrowingRepository).save(any(Borrowing.class));
        }
    }

    private Borrowing borrowing(Long id, String status, int availableCopies) {
        Book book = new Book();
        book.setId(99L);
        book.setTitle("Sample Book");
        book.setAvailableCopies(availableCopies);
        book.setTotalCopies(5);

        Borrowing borrowing = new Borrowing();
        borrowing.setId(id);
        borrowing.setBook(book);
        borrowing.setStatus(status);
        borrowing.setBorrowDate(LocalDate.now());
        borrowing.setDueDate(LocalDate.now().plusDays(14));
        return borrowing;
    }

    private Book book() {
        Book book = new Book();
        book.setTitle("Effective Java");
        book.setAuthor("Joshua Bloch");
        book.setTotalCopies(1);
        book.setAvailableCopies(1);
        return book;
    }

    private User user() {
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        Role role = new Role();
        role.setName("USER");
        user.setRoles(Set.of(role));
        return user;
    }
}
