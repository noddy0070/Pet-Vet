package org.springframework.samples.petclinic.api.pet;

import java.util.List;

import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PetApiService {

	private final OwnerRepository ownerRepository;

	public PetApiService(OwnerRepository ownerRepository) {
		this.ownerRepository = ownerRepository;
	}

	@Transactional(readOnly = true)
	public List<PetDto> listOwnerPets(Integer ownerId, String clinicId) {
		Owner owner = findOwner(ownerId);
		return owner.getPets().stream().map(pet -> toDto(pet, owner.getId(), clinicId)).toList();
	}

	@Transactional(readOnly = true)
	public PetDto getPet(Integer ownerId, Integer petId, String clinicId) {
		Owner owner = findOwner(ownerId);
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new ResponseStatusException(NOT_FOUND, "Pet not found for owner");
		}
		return toDto(pet, owner.getId(), clinicId);
	}

	@Transactional
	public PetDto createPet(CreatePetRequest request, String clinicId) {
		Owner owner = findOwner(request.ownerId());
		if (owner.getPet(request.name(), true) != null) {
			throw new ResponseStatusException(CONFLICT, "Pet with this name already exists for owner");
		}

		PetType petType = findPetType(request.petTypeId());
		Pet pet = new Pet();
		pet.setName(request.name());
		pet.setType(petType);
		pet.setBirthDate(request.birthDate());
		owner.addPet(pet);
		this.ownerRepository.save(owner);

		return toDto(pet, owner.getId(), clinicId);
	}

	@Transactional
	public PetDto updatePet(Integer petId, UpdatePetRequest request, String clinicId) {
		Owner owner = findOwner(request.ownerId());
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new ResponseStatusException(NOT_FOUND, "Pet not found for owner");
		}

		if (request.name() != null && !request.name().equalsIgnoreCase(pet.getName())
				&& owner.getPet(request.name(), true) != null) {
			throw new ResponseStatusException(CONFLICT, "Pet with this name already exists for owner");
		}

		if (request.name() != null) {
			pet.setName(request.name());
		}
		if (request.petTypeId() != null) {
			pet.setType(findPetType(request.petTypeId()));
		}
		if (request.birthDate() != null) {
			pet.setBirthDate(request.birthDate());
		}

		this.ownerRepository.save(owner);
		return toDto(pet, owner.getId(), clinicId);
	}

	private Owner findOwner(Integer ownerId) {
		Owner owner = this.ownerRepository.findById(ownerId);
		if (owner == null) {
			throw new ResponseStatusException(NOT_FOUND, "Owner not found");
		}
		return owner;
	}

	private PetType findPetType(Integer petTypeId) {
		return this.ownerRepository.findPetTypes()
			.stream()
			.filter(type -> petTypeId.equals(type.getId()))
			.findFirst()
			.orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pet type not found"));
	}

	private PetDto toDto(Pet pet, Integer ownerId, String clinicId) {
		String petType = pet.getType() == null ? null : pet.getType().getName();
		return new PetDto(pet.getId(), clinicId, ownerId, pet.getName(), petType, pet.getBirthDate());
	}

}