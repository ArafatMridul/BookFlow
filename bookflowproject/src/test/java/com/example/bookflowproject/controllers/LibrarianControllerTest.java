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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
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
            when(borrowingRepository.findByStatusOrderByBorrowDateDesc("RETURN_REQUESTED"))
                .thenReturn(List.of(borrowing(3L, "RETURN_REQUESTED", 0)));
            when(borrowingRepository.findByStatusInOrderByBorrowDateDesc(List.of("BORROWED", "OVERDUE")))
                .thenReturn(List.of(borrowing(2L, "BORROWED", 1)));

            mockMvc.perform(get("/librarian/borrowings"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/librarian-borrowings"))
                    .andExpect(model().attribute("username", "librarian"))
                .andExpect(model().attributeExists("requestedBorrowings", "returnRequestedBorrowings", "activeBorrowings"));
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
                void shouldUseSearchWhenManagingBooks() throws Exception {
                    when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("java", "java"))
                        .thenReturn(List.of(book()));

                    mockMvc.perform(get("/librarian/books/manage").param("search", "java"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("dashboard/librarian-books"))
                        .andExpect(model().attribute("search", "java"));
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
        void shouldRejectUpdateWhenBookMissing() throws Exception {
            when(bookRepository.findById(404L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/librarian/books/404/update")
                            .param("title", "Any")
                            .param("author", "Any"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/books/manage"))
                    .andExpect(flash().attribute("errorMsg", "Book not found."));

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectUpdateWhenTitleOrAuthorMissing() throws Exception {
            Book existing = book();
            existing.setId(1L);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

            mockMvc.perform(post("/librarian/books/1/update")
                            .param("title", "")
                            .param("author", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/books/manage?bookId=1"))
                    .andExpect(flash().attribute("errorMsg", "Title and author are required."));

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectUpdateWhenIsbnExistsOnAnotherBook() throws Exception {
            Book existing = book();
            existing.setId(1L);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(bookRepository.existsByIsbnAndIdNot("9780134685991", 1L)).thenReturn(true);

            mockMvc.perform(post("/librarian/books/1/update")
                            .param("title", "Effective Java")
                            .param("author", "Joshua Bloch")
                            .param("isbn", "9780134685991"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/books/manage?bookId=1"))
                    .andExpect(flash().attribute("errorMsg", "A book with this ISBN already exists."));

            verify(bookRepository, never()).save(any(Book.class));
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

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectDeleteWhenBookMissing() throws Exception {
            when(bookRepository.findById(404L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/librarian/books/404/delete"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/books/manage"))
                    .andExpect(flash().attribute("errorMsg", "Book not found."));

            verify(bookRepository, never()).delete(any(Book.class));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectDeleteWhenBorrowingRecordsExist() throws Exception {
            Book existing = book();
            existing.setId(2L);
            when(bookRepository.findById(2L)).thenReturn(Optional.of(existing));
            when(borrowingRepository.existsByBookId(2L)).thenReturn(true);

            mockMvc.perform(post("/librarian/books/2/delete"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/books/manage?bookId=2"))
                    .andExpect(flash().attribute("errorMsg", "This book cannot be deleted because borrowing records already exist."));

            verify(bookRepository, never()).delete(any(Book.class));
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
        void shouldRejectAddBookWhenTitleOrAuthorMissing() throws Exception {
            mockMvc.perform(post("/librarian/books/add")
                            .param("title", "")
                            .param("author", "")
                            .param("isbn", "9780134494166"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/books/add"))
                    .andExpect(flash().attribute("errorMsg", "Title and author are required."));

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectAddBookWhenDuplicateIsbn() throws Exception {
            when(bookRepository.existsByIsbn("9780134494166")).thenReturn(true);

            mockMvc.perform(post("/librarian/books/add")
                            .param("title", "Clean Architecture")
                            .param("author", "Robert C. Martin")
                            .param("isbn", "9780134494166"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/books/add"))
                    .andExpect(flash().attribute("errorMsg", "A book with this ISBN already exists."));

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldHandleRepositoryErrorWhenAddingBook() throws Exception {
            doThrow(new RuntimeException("db error")).when(bookRepository).save(any(Book.class));

            mockMvc.perform(post("/librarian/books/add")
                            .param("title", "Clean Architecture")
                            .param("author", "Robert C. Martin")
                            .param("isbn", "9780134494166")
                            .param("totalCopies", "3")
                            .param("availableCopies", "3"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/books/manage"))
                    .andExpect(flash().attributeExists("errorMsg"));
        }

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

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectApprovalWhenRequestMissingOrInvalidStatus() throws Exception {
            Borrowing borrowing = borrowing(12L, "BORROWED", 2);
            when(borrowingRepository.findById(12L)).thenReturn(Optional.of(borrowing));

            mockMvc.perform(post("/librarian/borrowings/12/approve"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/borrowings"))
                    .andExpect(flash().attribute("errorMsg", "Borrow request not found."));

            verify(borrowingRepository, never()).save(any(Borrowing.class));
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

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectWhenBorrowRequestMissing() throws Exception {
            when(borrowingRepository.findById(13L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/librarian/borrowings/13/reject"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/borrowings"))
                    .andExpect(flash().attribute("errorMsg", "Borrow request not found."));

            verify(borrowingRepository, never()).save(any(Borrowing.class));
        }
    }

    @Nested
    @DisplayName("POST /librarian/borrowings/{id}/process-return")
    class ProcessReturn {

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldProcessReturnAndIncreaseBookStock() throws Exception {
            Borrowing borrowing = borrowing(20L, "RETURN_REQUESTED", 1);
            when(borrowingRepository.findById(20L)).thenReturn(Optional.of(borrowing));

            mockMvc.perform(post("/librarian/borrowings/20/process-return"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/borrowings"))
                    .andExpect(flash().attribute("successMsg", "Return request processed successfully."));

            verify(borrowingRepository).save(any(Borrowing.class));
            verify(bookRepository).save(any(Book.class));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectProcessingWhenReturnRequestMissing() throws Exception {
            when(borrowingRepository.findById(21L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/librarian/borrowings/21/process-return"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/borrowings"))
                    .andExpect(flash().attribute("errorMsg", "Return request not found."));

            verify(borrowingRepository, never()).save(any(Borrowing.class));
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectProcessingWhenBookMissingFromBorrowing() throws Exception {
            Borrowing borrowing = borrowing(23L, "RETURN_REQUESTED", 1);
            borrowing.setBook(null);
            when(borrowingRepository.findById(23L)).thenReturn(Optional.of(borrowing));

            mockMvc.perform(post("/librarian/borrowings/23/process-return"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/borrowings"))
                    .andExpect(flash().attribute("errorMsg", "Book not found for this borrowing record."));

            verify(borrowingRepository, never()).save(any(Borrowing.class));
            verify(bookRepository, never()).save(any(Book.class));
        }
    }

    @Nested
    @DisplayName("POST /librarian/borrowings/{id}/reject-return")
    class RejectReturn {

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectReturnRequest() throws Exception {
            Borrowing borrowing = borrowing(22L, "RETURN_REQUESTED", 1);
            when(borrowingRepository.findById(22L)).thenReturn(Optional.of(borrowing));

            mockMvc.perform(post("/librarian/borrowings/22/reject-return"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/borrowings"))
                    .andExpect(flash().attribute("successMsg", "Return request rejected."));

            verify(borrowingRepository).save(any(Borrowing.class));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldRejectWhenReturnRequestMissing() throws Exception {
            when(borrowingRepository.findById(24L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/librarian/borrowings/24/reject-return"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/borrowings"))
                    .andExpect(flash().attribute("errorMsg", "Return request not found."));

            verify(borrowingRepository, never()).save(any(Borrowing.class));
        }

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        void shouldSetOverdueStatusWhenRejectingLateReturn() throws Exception {
            Borrowing borrowing = borrowing(25L, "RETURN_REQUESTED", 1);
            borrowing.setDueDate(LocalDate.now().minusDays(1));
            when(borrowingRepository.findById(25L)).thenReturn(Optional.of(borrowing));

            mockMvc.perform(post("/librarian/borrowings/25/reject-return"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/borrowings"))
                    .andExpect(flash().attribute("successMsg", "Return request rejected."));

            verify(borrowingRepository).save(argThat(saved -> "OVERDUE".equals(saved.getStatus())));
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
