package org.springframework.samples.petclinic.api.pet;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

public record CreatePetRequest(@NotNull Integer ownerId, @NotBlank String name, @NotNull Integer petTypeId,
		@PastOrPresent LocalDate birthDate) {
}