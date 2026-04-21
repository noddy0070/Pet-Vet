package org.springframework.samples.petclinic.api.pet;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record UpdatePetRequest(@NotNull Integer ownerId, @Size(min = 1, max = 30) String name, Integer petTypeId,
		@PastOrPresent LocalDate birthDate) {
}