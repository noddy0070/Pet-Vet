package org.springframework.samples.petclinic.api.security;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestIdResolver {

	public static final String REQUEST_ID_HEADER = "X-Request-Id";

	private RequestIdResolver() {
	}

	public static String resolve(HttpServletRequest request) {
		String requestId = request.getHeader(REQUEST_ID_HEADER);
		if (requestId == null || requestId.isBlank()) {
			return UUID.randomUUID().toString();
		}
		return requestId;
	}

}