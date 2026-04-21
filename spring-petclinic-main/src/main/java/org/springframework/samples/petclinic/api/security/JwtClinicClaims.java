package org.springframework.samples.petclinic.api.security;

import java.util.List;

import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

public record JwtClinicClaims(String userId, String role, List<String> clinicIds) {

	public String primaryClinicId() {
		if (this.clinicIds == null || this.clinicIds.isEmpty()) {
			throw new ResponseStatusException(FORBIDDEN, "User is not assigned to any clinic");
		}
		return this.clinicIds.get(0);
	}

}