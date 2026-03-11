package com.example.bookflowproject.repository;

import com.example.bookflowproject.entity.Borrowing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {
    long countByStatus(String status);
    long countByUserUsernameAndStatus(String username, String status);
}

