package org.springframework.samples.petclinic.vet.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OwnerRestController.class)
public class OwnerRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OwnerRepository ownerRepository;

	@Test
	public void testFindOwners() throws Exception {
		Owner owner = new Owner();
		owner.setLastName("Doe");

		Page<Owner> ownerPage = new PageImpl<>(Collections.singletonList(owner), PageRequest.of(0, 5), 1);
		when(ownerRepository.findByLastName("Doe", PageRequest.of(0, 5))).thenReturn(ownerPage);

		mockMvc.perform(
				get("/api/owners/find").param("page", "1").param("lastName", "Doe").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void testFindOwnersNotFound() throws Exception {
		Page<Owner> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 5), 0);
		when(ownerRepository.findByLastName("Unknown", PageRequest.of(0, 5))).thenReturn(emptyPage);

		mockMvc
			.perform(get("/api/owners/find").param("page", "1")
				.param("lastName", "Unknown")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNoContent());
	}

}