package org.springframework.samples.petclinic.vet.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
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

        when(ownerRepository.findByLastName("Doe")).thenReturn(Collections.singletonList(owner));

        mockMvc.perform(get("/api/owners/find")
                .param("lastName", "Doe")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testFindOwnersNotFound() throws Exception {
        when(ownerRepository.findByLastName("Unknown")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/owners/find")
                .param("lastName", "Unknown")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
