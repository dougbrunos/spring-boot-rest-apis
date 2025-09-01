package com.github.dougbrunos.services;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.github.dougbrunos.model.Person;

@Service
public class PersonServices {

    private final AtomicLong counter = new AtomicLong();

    private Logger logger = Logger.getLogger(PersonServices.class.getName());

    public Person findById(String id) {
        logger.info("Finding one Person!");

        Person person = new Person();
        person.setId(counter.incrementAndGet());
        person.setFirstName("Douglas");
        person.setLastName("Souza");
        person.setAddress("Cascavel - Parana - Brazil");
        person.setGender("Male");
        return person;
    }

}
