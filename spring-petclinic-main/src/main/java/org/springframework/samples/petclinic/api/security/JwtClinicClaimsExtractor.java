package org.springframework.samples.petclinic.api.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
public class JwtClinicClaimsExtractor {

	private static final String BEARER_PREFIX = "Bearer ";

	private final ObjectMapper objectMapper;

	public JwtClinicClaimsExtractor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public JwtClinicClaims extract(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new ResponseStatusException(UNAUTHORIZED, "Missing or invalid Authorization header");
		}

		String token = authorizationHeader.substring(BEARER_PREFIX.length());
		String[] sections = token.split("\\.");
		if (sections.length < 2) {
			throw new ResponseStatusException(UNAUTHORIZED, "Invalid JWT format");
		}

		try {
			String payloadJson = new String(Base64.getUrlDecoder().decode(sections[1]), StandardCharsets.UTF_8);
			JsonNode payload = this.objectMapper.readTree(payloadJson);

			String userId = textOrNull(payload.get("sub"));
			String role = textOrNull(payload.get("role"));
			List<String> clinicIds = readClinicIds(payload);

			return new JwtClinicClaims(userId, role, clinicIds);
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(UNAUTHORIZED, "JWT payload is not valid base64", ex);
		}
		catch (Exception ex) {
			throw new ResponseStatusException(UNAUTHORIZED, "Unable to parse JWT payload", ex);
		}
	}

	private List<String> readClinicIds(JsonNode payload) {
		List<String> clinicIds = new ArrayList<>();
		JsonNode arrayNode = payload.get("clinicIds");
		if (arrayNode == null) {
			arrayNode = payload.get("clinic_ids");
		}

		if (arrayNode != null && arrayNode.isArray()) {
			for (JsonNode clinicIdNode : arrayNode) {
				if (clinicIdNode.isTextual() && !clinicIdNode.asText().isBlank()) {
					clinicIds.add(clinicIdNode.asText());
				}
			}
		}
		return clinicIds;
	}

	private String textOrNull(JsonNode node) {
		return node == null ? null : node.asText(null);
	}

}