package org.springframework.samples.petclinic.api.pet;

import java.time.LocalDate;

public record PetDto(Integer id, String clinicId, Integer ownerId, String name, String petType, LocalDate birthDate) {
}