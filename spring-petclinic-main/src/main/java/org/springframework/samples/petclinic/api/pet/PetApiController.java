package org.springframework.samples.petclinic.api.pet;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.api.response.StandardResponse;
import org.springframework.samples.petclinic.api.security.JwtClinicClaims;
import org.springframework.samples.petclinic.api.security.JwtClinicClaimsExtractor;
import org.springframework.samples.petclinic.api.security.RequestIdResolver;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/pets")
public class PetApiController {

	private final PetApiService petApiService;

	private final JwtClinicClaimsExtractor claimsExtractor;

	public PetApiController(PetApiService petApiService, JwtClinicClaimsExtractor claimsExtractor) {
		this.petApiService = petApiService;
		this.claimsExtractor = claimsExtractor;
	}

	@GetMapping
	public ResponseEntity<StandardResponse<List<PetDto>>> listOwnerPets(
			@RequestHeader("Authorization") String authorizationHeader, @RequestParam @NotNull Integer ownerId,
			HttpServletRequest request) {
		JwtClinicClaims claims = this.claimsExtractor.extract(authorizationHeader);
		String clinicId = claims.primaryClinicId();
		String requestId = RequestIdResolver.resolve(request);
		List<PetDto> pets = this.petApiService.listOwnerPets(ownerId, clinicId);
		return ResponseEntity.ok(StandardResponse.success(pets, requestId));
	}

	@GetMapping("/{petId}")
	public ResponseEntity<StandardResponse<PetDto>> getPet(
			@RequestHeader("Authorization") String authorizationHeader, @PathVariable Integer petId,
			@RequestParam @NotNull Integer ownerId, HttpServletRequest request) {
		JwtClinicClaims claims = this.claimsExtractor.extract(authorizationHeader);
		String clinicId = claims.primaryClinicId();
		String requestId = RequestIdResolver.resolve(request);
		PetDto pet = this.petApiService.getPet(ownerId, petId, clinicId);
		return ResponseEntity.ok(StandardResponse.success(pet, requestId));
	}

	@PostMapping
	public ResponseEntity<StandardResponse<PetDto>> createPet(
			@RequestHeader("Authorization") String authorizationHeader, @Valid @RequestBody CreatePetRequest requestBody,
			HttpServletRequest request) {
		JwtClinicClaims claims = this.claimsExtractor.extract(authorizationHeader);
		String clinicId = claims.primaryClinicId();
		String requestId = RequestIdResolver.resolve(request);
		PetDto createdPet = this.petApiService.createPet(requestBody, clinicId);
		return ResponseEntity.status(201).body(StandardResponse.success(createdPet, requestId));
	}

	@PutMapping("/{petId}")
	public ResponseEntity<StandardResponse<PetDto>> updatePet(
			@RequestHeader("Authorization") String authorizationHeader, @PathVariable Integer petId,
			@Valid @RequestBody UpdatePetRequest requestBody, HttpServletRequest request) {
		JwtClinicClaims claims = this.claimsExtractor.extract(authorizationHeader);
		String clinicId = claims.primaryClinicId();
		String requestId = RequestIdResolver.resolve(request);
		PetDto updatedPet = this.petApiService.updatePet(petId, requestBody, clinicId);
		return ResponseEntity.ok(StandardResponse.success(updatedPet, requestId));
	}

}