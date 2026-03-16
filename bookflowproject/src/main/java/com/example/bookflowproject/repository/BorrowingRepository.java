package com.example.bookflowproject.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bookflowproject.entity.Borrowing;

@Repository
public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {
    long countByStatus(String status);
    long countByUserUsernameAndStatus(String username, String status);
    List<Borrowing> findByStatusOrderByBorrowDateDesc(String status);
    List<Borrowing> findByUserUsernameOrderByBorrowDateDesc(String username);
    boolean existsByUserIdAndBookIdAndStatusIn(Long userId, Long bookId, Collection<String> statuses);
}

