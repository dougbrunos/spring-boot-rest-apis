package com.github.dougbrunos.mapper.custom;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.github.dougbrunos.data.dto.v2.PersonDTOV2;
import com.github.dougbrunos.model.Person;

@Service
public class PersonMapper {

    public PersonDTOV2 convertEntityToDTO(Person person) {
        PersonDTOV2 dto = new PersonDTOV2();

        dto.setId(person.getId());
        dto.setFirstName(person.getFirstName());
        dto.setLastName(person.getLastName());
        dto.setBirthDay(new Date());
        dto.setAddress(person.getAddress());
        dto.setGender(person.getGender());

        return dto;
    }

    public Person convertDTOtoEntity(PersonDTOV2 person) {
        Person dto = new Person();

        dto.setId(person.getId());
        dto.setFirstName(person.getFirstName());
        dto.setLastName(person.getLastName());
        dto.setAddress(person.getAddress());
        dto.setGender(person.getGender());

        return dto;
    }
}
