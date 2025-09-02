package com.github.dougbrunos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.dougbrunos.model.Person;


public interface PersonRepository extends JpaRepository<Person, Long> {}