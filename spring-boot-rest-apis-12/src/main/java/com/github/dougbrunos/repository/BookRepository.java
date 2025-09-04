package com.github.dougbrunos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.dougbrunos.model.Book;

public interface BookRepository extends JpaRepository<Book, Long>{}