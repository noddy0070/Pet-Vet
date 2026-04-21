package org.springframework.samples.petclinic.api.response;

import java.time.Instant;

public record StandardResponse<T>(boolean success, T data, Instant timestamp, String requestId, String error) {

	public static <T> StandardResponse<T> success(T data, String requestId) {
		return new StandardResponse<>(true, data, Instant.now(), requestId, null);
	}

	public static <T> StandardResponse<T> error(String error, String requestId) {
		return new StandardResponse<>(false, null, Instant.now(), requestId, error);
	}

}