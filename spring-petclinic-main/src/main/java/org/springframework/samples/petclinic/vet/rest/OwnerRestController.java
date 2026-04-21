package org.springframework.samples.petclinic.vet.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.BindingResult;

import java.util.List;

@RestController
@RequestMapping("/api/owners")
public class OwnerRestController {

    private final OwnerRepository ownerRepository;

    @Autowired
    public OwnerRestController(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @GetMapping("/find")
    public ResponseEntity<List<Owner>> findOwners(@RequestParam(defaultValue = "1") int page, @RequestParam(required = false) String lastName, BindingResult result) {
        if (lastName == null) {
            lastName = ""; // empty string signifies broadest possible search
        }

        Pageable pageable = PageRequest.of(page - 1, 5);
        Page<Owner> ownersResults = ownerRepository.findByLastName(lastName, pageable);

        if (ownersResults.isEmpty()) {
            result.rejectValue("lastName", "notFound", "not found");
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(ownersResults.getContent());
    }
}