package com.example.bookflowproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bookflowproject.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    java.util.List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);
}

