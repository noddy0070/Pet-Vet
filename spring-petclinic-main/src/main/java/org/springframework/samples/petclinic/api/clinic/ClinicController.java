package org.springframework.samples.petclinic.api.clinic;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.api.response.StandardResponse;
import org.springframework.samples.petclinic.api.security.JwtClinicClaims;
import org.springframework.samples.petclinic.api.security.JwtClinicClaimsExtractor;
import org.springframework.samples.petclinic.api.security.RequestIdResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinics")
public class ClinicController {

	private final JwtClinicClaimsExtractor claimsExtractor;

	public ClinicController(JwtClinicClaimsExtractor claimsExtractor) {
		this.claimsExtractor = claimsExtractor;
	}

	@GetMapping
	public ResponseEntity<StandardResponse<List<ClinicDto>>> getClinics(
			@RequestHeader("Authorization") String authorizationHeader, HttpServletRequest request) {
		JwtClinicClaims claims = this.claimsExtractor.extract(authorizationHeader);
		String requestId = RequestIdResolver.resolve(request);
		List<ClinicDto> clinics = claims.clinicIds().stream().map(ClinicDto::new).toList();
		return ResponseEntity.ok(StandardResponse.success(clinics, requestId));
	}

}