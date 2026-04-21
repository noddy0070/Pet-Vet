# AS-IS DETAILED ARCHITECTURE ANALYSIS
## Spring PetClinic - Monolithic Architecture

**Document Version:** 1.0  
**Date:** April 21, 2026  
**Project:** Pet-Vet (Spring PetClinic)  
**Analysis Scope:** Complete architectural assessment

---

## 1. ARCHITECTURE PATTERN

### Current Pattern: **Monolithic MVC (Model-View-Controller)**

```
┌─────────────────────────────────────────────────────────────────┐
│                   SINGLE DEPLOYABLE UNIT                         │
│                 (spring-petclinic.jar / .war)                    │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  PRESENTATION LAYER (View)                               │   │
│  │  - Thymeleaf Templates (HTML)                            │   │
│  │  - Static Resources (CSS, JS, Images)                    │   │
│  │  - Bootstrap 5.3.3 UI Framework                          │   │
│  └──────────────────────────────────────────────────────────┘   │
│                            ↕ (tight coupling)                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  CONTROLLER LAYER (Handler)                              │   │
│  │  - OwnerController, PetController, VisitController       │   │
│  │  - VetController, WelcomeController                      │   │
│  │  - OwnerRestController (API)                             │   │
│  │  - Spring MVC Request Routing                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                            ↕                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  SERVICE/BUSINESS LOGIC LAYER                            │   │
│  │  - JPA Repository Interfaces                             │   │
│  │  - Validation Rules                                      │   │
│  │  - Cache Configuration (Caffeine)                        │   │
│  │  - Transaction Management                                │   │
│  └──────────────────────────────────────────────────────────┘   │
│                            ↕                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  DATA ACCESS LAYER (Persistence)                         │   │
│  │  - Hibernate ORM                                         │   │
│  │  - Spring Data JPA                                       │   │
│  │  - Entity Mapping                                        │   │
│  └──────────────────────────────────────────────────────────┘   │
│                            ↕                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  DATABASE LAYER                                          │   │
│  │  - H2 (In-Memory, Default)                               │   │
│  │  - MySQL 8.4+ (Optional)                                 │   │
│  │  - PostgreSQL 16.3+ (Optional)                           │   │
│  │  - Single Shared Database Instance                       │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Monolithic Characteristics:

✅ **Single Codebase:** All features in one Git repository  
✅ **Single Process:** One Java process running all services  
✅ **Shared Database:** All modules access same DB schema  
✅ **Tight Coupling:** Controllers directly depend on repositories  
✅ **Single Deployment:** Full app redeployment for any change  
✅ **Shared Technology Stack:** All components use Java + Spring Boot  

---

## 2. MODULE BREAKDOWN

### 2.1 Owner Module (`owner/`)
**Responsibility:** Manage pet owners and their information

**Components:**
- `Owner.java` - Entity with @OneToMany relationship to Pets
- `OwnerController.java` - MVC Controller for owner management
- `OwnerRepository.java` - Spring Data JPA Repository
- `Pet.java` - Entity for pet information
- `PetController.java` - MVC Controller for pet management
- `PetType.java` - Reference data for pet types
- `PetTypeFormatter.java` - Type conversion for form data
- `PetValidator.java` - Validation logic
- `Visit.java` - Entity for visit records
- `VisitController.java` - MVC Controller for visit scheduling

**Key Responsibilities:**
- CRUD operations on owners (Create, Read, Update)
- Search owners by last name with pagination
- Pet registration and management
- Visit scheduling and tracking

### 2.2 Vet Module (`vet/`)
**Responsibility:** Manage veterinary professionals and their specialties

**Components:**
- `Vet.java` - Entity with @ManyToMany relationship to Specialties
- `VetController.java` - MVC Controller (both HTML and JSON)
- `VetRepository.java` - Spring Data JPA Repository with caching
- `Specialty.java` - Reference entity for veterinary specialties
- `rest/OwnerRestController.java` - REST API endpoint

**Key Responsibilities:**
- Display list of veterinarians
- Show vet specialties (Radiology, Surgery, Dentistry)
- Provide REST API for vet data
- Cache vet information for performance

### 2.3 System Module (`system/`)
**Responsibility:** Application-level services and configurations

**Components:**
- `CacheConfiguration.java` - Caffeine cache setup
- `WelcomeController.java` - Home page handler
- `CrashController.java` - Error demonstration endpoint

**Key Responsibilities:**
- Configure caching strategy
- Handle application startup page
- Demonstrate error handling

### 2.4 Model Module (`model/`)
**Responsibility:** Base classes and common entities

**Components:**
- `BaseEntity.java` - Base class with ID field
- `NamedEntity.java` - Extended base with name field
- `Person.java` - Base for Owner and Vet entities

**Key Responsibilities:**
- Provide common inheritance hierarchy
- Define shared entity properties

---

## 3. ENDPOINT MAPPING

### 3.1 Owner Management Endpoints

```
GET  /owners/new                 → initCreationForm()
POST /owners/new                 → processCreationForm()
GET  /owners/find                → initFindForm()
GET  /owners                      → processFindForm() [search + pagination]
GET  /owners/{ownerId}            → showOwner() [display details]
GET  /owners/{ownerId}/edit       → initUpdateOwnerForm()
POST /owners/{ownerId}/edit       → processUpdateOwnerForm()
```

### 3.2 Pet Management Endpoints

```
GET  /owners/{ownerId}/pets/new   → initCreationForm()
POST /owners/{ownerId}/pets/new   → processCreationForm()
GET  /owners/{ownerId}/pets/{petId}/edit  → initUpdateForm()
POST /owners/{ownerId}/pets/{petId}/edit  → processUpdateForm()
```

### 3.3 Visit Management Endpoints

```
GET  /owners/{ownerId}/pets/{petId}/visits/new    → initNewVisitForm()
POST /owners/{ownerId}/pets/{petId}/visits/new    → processNewVisitForm()
```

### 3.4 Vet Management Endpoints

```
GET  /vets.html                   → showVetList() [HTML with pagination]
GET  /vets                        → showResourcesVetList() [JSON response]
```

### 3.5 REST API Endpoints (New)

```
GET  /api/owners/find?page=1&lastName=Davis  → findOwners() [JSON response]
```

### 3.6 System Endpoints

```
GET  /                            → welcome()
GET  /oups                        → triggerException() [error demo]
```

---

## 4. ENTITY RELATIONSHIPS & DATA MODEL

### 4.1 Entity Relationship Diagram (ER)

```
┌─────────────────────────┐
│      OWNERS             │
├─────────────────────────┤
│ PK  id                  │
│     first_name          │
│     last_name           │
│     address             │
│     city                │
│     telephone           │
└───────────┬─────────────┘
            │ (1:N) cascade
            │ fetch=EAGER
            ↓
┌─────────────────────────┐       ┌──────────────────────┐
│      PETS               │───────→│   PET_TYPES          │
├─────────────────────────┤ FK    ├──────────────────────┤
│ PK  id                  │ (M:1) │ PK  id               │
│     name                │       │     name             │
│     birth_date          │       │ (cat, dog, etc.)     │
│ FK  type_id    ──────┐  │       └──────────────────────┘
│ FK  owner_id   ──────┼──┤
│                      │  │
│                      │  └──────→[TYPES TABLE]
└───────────┬──────────┘
            │ (1:N) cascade
            │ fetch=EAGER
            ↓
┌─────────────────────────┐
│      VISITS             │
├─────────────────────────┤
│ PK  id                  │
│     visit_date          │
│     description         │
│ FK  pet_id              │
└─────────────────────────┘


┌──────────────────────────┐       ┌────────────────────────┐
│       VETS               │       │   SPECIALTIES          │
├──────────────────────────┤       ├────────────────────────┤
│ PK  id                   │       │ PK  id                 │
│     first_name           │       │     name               │
│     last_name            │       │ (radiology, surgery,   │
└────────┬─────────────────┘       │  dentistry)            │
         │                         └──────────┬─────────────┘
         │ (M:N join table)                  │
         └────────────────────────────────────┘
                VET_SPECIALTIES
            (vet_id, specialty_id)
```

### 4.2 JPA Relationship Details

**Owner → Pets (One-to-Many)**
```java
@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
@JoinColumn(name = "owner_id")
@OrderBy("name")
private List<Pet> pets = new ArrayList<>();
```
- Cascade: ALL (delete owner → delete pets)
- Fetch: EAGER (load pets immediately)
- Order: Alphabetically by name

**Pet → Visits (One-to-Many)**
```java
@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
@JoinColumn(name = "pet_id")
@OrderBy("visit_date ASC")
private Set<Visit> visits = new LinkedHashSet<>();
```
- Cascade: ALL (delete pet → delete visits)
- Fetch: EAGER (load visits immediately)
- Order: By visit date ascending

**Pet → PetType (Many-to-One)**
```java
@ManyToOne
@JoinColumn(name = "type_id")
private PetType type;
```
- Lazy loading (default)
- Foreign key reference

**Vet → Specialty (Many-to-Many)**
```
VET_SPECIALTIES join table with:
  - vet_id (FK to vets)
  - specialty_id (FK to specialties)
```

---

## 5. DATABASE SCHEMA

### 5.1 Tables Overview

| Table | Purpose | Rows | Relationships |
|-------|---------|------|--------------|
| `owners` | Pet owners | 10 | Parent for pets |
| `pets` | Pet records | 13 | Child of owners, parent of visits |
| `visits` | Medical records | 4 | Child of pets |
| `vets` | Veterinarians | 6 | Join to specialties |
| `specialties` | Vet skills | 3 | Join from vets |
| `vet_specialties` | Vet-Specialty mapping | 5 | Many-to-many join |
| `types` | Pet types | 6 | Referenced by pets |

### 5.2 Schema Definition (H2 Example)

```sql
-- OWNERS TABLE
CREATE TABLE owners (
  id         INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  first_name VARCHAR(30),
  last_name  VARCHAR_IGNORECASE(30),
  address    VARCHAR(255),
  city       VARCHAR(80),
  telephone  VARCHAR(20)
);
CREATE INDEX owners_last_name ON owners (last_name);

-- PETS TABLE
CREATE TABLE pets (
  id         INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  name       VARCHAR(30),
  birth_date DATE,
  type_id    INTEGER NOT NULL,
  owner_id   INTEGER
);
ALTER TABLE pets ADD CONSTRAINT fk_pets_owners 
  FOREIGN KEY (owner_id) REFERENCES owners (id);
ALTER TABLE pets ADD CONSTRAINT fk_pets_types 
  FOREIGN KEY (type_id) REFERENCES types (id);
CREATE INDEX pets_name ON pets (name);

-- VISITS TABLE
CREATE TABLE visits (
  id          INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  pet_id      INTEGER,
  visit_date  DATE,
  description VARCHAR(255)
);
ALTER TABLE visits ADD CONSTRAINT fk_visits_pets 
  FOREIGN KEY (pet_id) REFERENCES pets (id);
CREATE INDEX visits_pet_id ON visits (pet_id);

-- VETS TABLE
CREATE TABLE vets (
  id         INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  first_name VARCHAR(30),
  last_name  VARCHAR(30)
);
CREATE INDEX vets_last_name ON vets (last_name);

-- SPECIALTIES TABLE
CREATE TABLE specialties (
  id   INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  name VARCHAR(80)
);
CREATE INDEX specialties_name ON specialties (name);

-- VET_SPECIALTIES JUNCTION TABLE
CREATE TABLE vet_specialties (
  vet_id       INTEGER NOT NULL,
  specialty_id INTEGER NOT NULL
);
ALTER TABLE vet_specialties ADD CONSTRAINT fk_vet_specialties_vets 
  FOREIGN KEY (vet_id) REFERENCES vets (id);
ALTER TABLE vet_specialties ADD CONSTRAINT fk_vet_specialties_specialties 
  FOREIGN KEY (specialty_id) REFERENCES specialties (id);

-- TYPES TABLE (Reference Data)
CREATE TABLE types (
  id   INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  name VARCHAR(80)
);
CREATE INDEX types_name ON types (name);
```

### 5.3 Sample Data Populated

- **10 Owners:** George Franklin, Betty Davis, Eduardo Rodriguez, etc.
- **13 Pets:** Leo (cat), Basil (hamster), Rosy (dog), Iggy (lizard), etc.
- **6 Vets:** James Carter, Helen Leary, Linda Douglas, etc.
- **3 Specialties:** Radiology, Surgery, Dentistry
- **4 Visits:** Rabies shots, neutered, spayed procedures

### 5.4 Multi-Database Support

Application supports switching databases via Spring profiles:

**H2 (Default - In-Memory)**
- Automatically recreated on startup
- File: `src/main/resources/db/h2/schema.sql`

**MySQL 8.4+**
- Profile: `spring.profiles.active=mysql`
- Setup: `src/main/resources/db/mysql/petclinic_db_setup_mysql.txt`
- User creation: `src/main/resources/db/mysql/user.sql`

**PostgreSQL 16.3+**
- Profile: `spring.profiles.active=postgres`
- Setup: `src/main/resources/db/postgres/petclinic_db_setup_postgres.txt`

---

## 6. REQUEST-RESPONSE FLOW

### 6.1 Complete Flow Example: View Owner Details

```
┌─────────────────────────────────────────────────────────────────┐
│  STEP 1: Browser Request                                        │
├─────────────────────────────────────────────────────────────────┤
│  GET /owners/1  HTTP/1.1                                        │
│  Host: localhost:8080                                           │
│  Accept: text/html                                              │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│  STEP 2: Spring DispatcherServlet                               │
├─────────────────────────────────────────────────────────────────┤
│  - Routes request based on URL mapping                          │
│  - Identifies handler: OwnerController.showOwner()              │
│  - Extracts @PathVariable: ownerId = 1                          │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│  STEP 3: OwnerController.showOwner(1)                           │
├─────────────────────────────────────────────────────────────────┤
│  @GetMapping("/owners/{ownerId}")                               │
│  public ModelAndView showOwner(@PathVariable("ownerId") int id) │
│  {                                                              │
│    ModelAndView mav = new ModelAndView("owners/ownerDetails"); │
│    Owner owner = ownerRepository.findById(id);                 │
│    mav.addObject(owner);                                       │
│    return mav;                                                 │
│  }                                                              │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│  STEP 4: OwnerRepository.findById(1)                            │
├─────────────────────────────────────────────────────────────────┤
│  @Query("SELECT owner FROM Owner owner                          │
│           left join fetch owner.pets                            │
│           WHERE owner.id = :id")                                │
│  Owner findById(@Param("id") Integer id);                       │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│  STEP 5: Hibernate ORM                                          │
├─────────────────────────────────────────────────────────────────┤
│  - Translates JPA Query to SQL                                  │
│  - Executes FETCH JOIN (eager load pets)                        │
│  - Retrieves Owner entity + all related Pets                    │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│  STEP 6: Database Query Execution                               │
├─────────────────────────────────────────────────────────────────┤
│  SELECT o.*, p.*                                                │
│  FROM owners o                                                  │
│  LEFT JOIN pets p ON o.id = p.owner_id                          │
│  WHERE o.id = 1                                                 │
│                                                                  │
│  ┌─────────────────────────────────────────────────┐            │
│  │ OWNERS                                          │            │
│  ├─────┬──────────┬───────────┬─────────┬─────────┤            │
│  │ ID  │ FIRST    │ LAST      │ ADDRESS │ CITY    │            │
│  ├─────┼──────────┼───────────┼─────────┼─────────┤            │
│  │  1  │ George   │ Franklin  │ 110 ... │ Madison │            │
│  └─────┴──────────┴───────────┴─────────┴─────────┘            │
│                                                                  │
│  ┌────────────────────────────────────┐                        │
│  │ PETS (related to owner 1)          │                        │
│  ├──────┬────────────┬──────────────┤                        │
│  │ ID   │ NAME       │ OWNER_ID     │                        │
│  ├──────┼────────────┼──────────────┤                        │
│  │  1   │ Leo        │      1       │                        │
│  └──────┴────────────┴──────────────┘                        │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│  STEP 7: Entity Mapping                                         │
├─────────────────────────────────────────────────────────────────┤
│  Hibernate creates:                                             │
│  - Owner(id=1, firstName="George", pets=[Pet(1, "Leo")])       │
│  - Pet(id=1, name="Leo", owner=Owner{1}, visits=[])            │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│  STEP 8: ModelAndView Population                                │
├─────────────────────────────────────────────────────────────────┤
│  model.addAttribute("owner", owner);                            │
│  model.addAttribute("org.springframework.validation.             │
│                      BindingResult.owner", ...);                │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│  STEP 9: Thymeleaf Template Rendering                           │
├─────────────────────────────────────────────────────────────────┤
│  Template: owners/ownerDetails.html                             │
│  - Resolves to: classpath:templates/owners/ownerDetails.html    │
│  - Processes model data:                                        │
│    * Owner name: "George Franklin"                              │
│    * Address: "110 W. Liberty St."                              │
│    * Pets list: [Leo]                                           │
│    * Visits per pet: [4 visits for Leo]                         │
│  - Generates HTML markup                                        │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│  STEP 10: HTTP Response                                         │
├─────────────────────────────────────────────────────────────────┤
│  HTTP/1.1 200 OK                                                │
│  Content-Type: text/html;charset=UTF-8                          │
│                                                                  │
│  <html>                                                         │
│    <body>                                                       │
│      <h2>Owner Information</h2>                                 │
│      <table>                                                    │
│        <tr><th>Name:</th><td>George Franklin</td></tr>         │
│        <tr><th>Address:</th><td>110 W. Liberty St.</td></tr>   │
│      </table>                                                   │
│      <h3>Pets and Visits</h3>                                   │
│      <p>Pet: Leo</p>                                            │
│      ...                                                        │
│    </body>                                                      │
│  </html>                                                        │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│  STEP 11: Browser Rendering                                     │
├─────────────────────────────────────────────────────────────────┤
│  - Browser parses HTML                                          │
│  - Loads CSS (Bootstrap 5.3.3)                                  │
│  - Renders owner details page                                   │
│  - Displays pets and visit history                              │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 Search Owner Flow (With Pagination)

```
GET /owners?page=1&lastName=D

1. OwnerController.processFindForm(1, owner, result, model)
2. Repository Query:
   SELECT DISTINCT owner FROM Owner owner 
   LEFT JOIN owner.pets 
   WHERE owner.lastName LIKE 'D%'
   
3. Database returns:
   - Betty Davis (ID=2)
   - Harold Davis (ID=4)
   
4. Pagination created:
   - Total Elements: 2
   - Current Page: 1
   - Page Size: 5
   - Total Pages: 1
   
5. Return template: owners/ownersList.html
   - Model contains: listOwners, currentPage, totalPages
```

### 6.3 Create New Owner Flow

```
GET /owners/new
├─ Show empty form (owners/createOrUpdateOwnerForm.html)
│
POST /owners/new
├─ Validate input (Jakarta Validation)
├─ If errors:
│  └─ Return form with validation messages
├─ If valid:
│  ├─ ownerRepository.save(owner)
│  ├─ Database INSERT
│  └─ Redirect to /owners/{newOwnerId}
```

---

## 7. TECHNOLOGY STACK

### 7.1 Core Framework

| Component | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 3.3.2 | Application framework |
| Spring Framework | 6.x | Core IoC container |
| Spring Data JPA | Latest | Repository abstraction |
| Spring Web MVC | Latest | Web framework |
| Hibernate | 6.x | ORM provider |

### 7.2 Language & Runtime

| Component | Version |
|-----------|---------|
| Java | 17+ |
| Jakarta EE | 10.0 |
| Servlet API | 5.x |

### 7.3 Database Support

| Database | Version | Profile |
|----------|---------|---------|
| H2 | Latest | (default) |
| MySQL | 8.4+ | `mysql` |
| PostgreSQL | 16.3+ | `postgres` |
| HSQLDB | Supported | (fallback) |

### 7.4 View & Frontend

| Component | Version |
|-----------|---------|
| Thymeleaf | 3.x |
| Bootstrap | 5.3.3 |
| Font Awesome | 4.7 |
| WebJars | Bundled |

### 7.5 Additional Libraries

| Purpose | Library |
|---------|---------|
| Caching | Caffeine Cache |
| Validation | Jakarta Bean Validation |
| Monitoring | Spring Boot Actuator |
| i18n | Spring Messages |
| Formatting | Spring Format |
| Build | Maven 3.x / Gradle |

---

## 8. CACHING STRATEGY

### 8.1 Cache Configuration (Caffeine)

```java
@Configuration
public class CacheConfiguration {
    @Bean
    public CacheManager cacheManager() {
        // Caffeine cache configuration
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("vets");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES));
        return cacheManager;
    }
}
```

### 8.2 Cached Queries

```java
@Transactional(readOnly = true)
@Cacheable("vets")
Collection<Vet> findAll();

@Transactional(readOnly = true)
@Cacheable("vets")
Page<Vet> findAll(Pageable pageable);
```

**Cache Impact:**
- Vets data cached for 10 minutes
- Reduces database queries
- Improves response time for vet listings

---

## 9. VALIDATION & ERROR HANDLING

### 9.1 Entity-Level Validation

```java
// Owner Entity
@NotBlank
private String address;

@NotBlank
private String city;

@NotBlank
@Pattern(regexp = "\\d{10}", message = "Telephone must be a 10-digit number")
private String telephone;

// Visit Entity
@NotBlank
private String description;
```

### 9.2 Business Logic Validation

```java
// PetValidator
if (StringUtils.hasText(pet.getName()) && pet.isNew() 
    && owner.getPet(pet.getName(), true) != null) {
    result.rejectValue("name", "duplicate", "already exists");
}

// Birth date validation
LocalDate currentDate = LocalDate.now();
if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
    result.rejectValue("birthDate", "typeMismatch.birthDate");
}
```

### 9.3 Error Handling

- **CrashController** - Demonstrates exception handling
- **error.html** - Custom error page template
- **Spring actuator** - Endpoint exposure for monitoring

---

## 10. CONFIGURATION & CUSTOMIZATION

### 10.1 Application Properties

```properties
# Database
database=h2
spring.sql.init.schema-locations=classpath*:db/${database}/schema.sql
spring.sql.init.data-locations=classpath*:db/${database}/data.sql

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=true

# Web
spring.thymeleaf.mode=HTML

# i18n
spring.messages.basename=messages/messages

# Actuator
management.endpoints.web.exposure.include=*

# Caching
spring.cache.type=caffeine

# Logging
logging.level.org.springframework=INFO
```

### 10.2 Multi-Language Support (i18n)

Files: `messages_en.properties`, `messages_de.properties`, `messages_es.properties`, `messages_ko.properties`

**Supported Languages:**
- English (en)
- German (de)
- Spanish (es)
- Korean (ko)

### 10.3 Profile-Based Configuration

```bash
# Default (H2)
java -jar spring-petclinic.jar

# MySQL
java -jar spring-petclinic.jar --spring.profiles.active=mysql

# PostgreSQL
java -jar spring-petclinic.jar --spring.profiles.active=postgres
```

---

## 11. MONOLITHIC CHARACTERISTICS

### 11.1 Tight Coupling Examples

**Direct Repository Dependency in Controller:**
```java
@Controller
class OwnerController {
    private final OwnerRepository owners;
    
    public OwnerController(OwnerRepository owners) {
        this.owners = owners;  // Direct dependency
    }
}
```

**Service Logic Embedded in Controller:**
```java
@PostMapping("/owners/new")
public String processCreationForm(@Valid Owner owner, BindingResult result) {
    this.owners.save(owner);  // Business logic in controller
    return "redirect:/owners/" + owner.getId();
}
```

**Template Directly Coupled to Controller:**
```
Controller returns: "owners/createOrUpdateOwnerForm"
Thymeleaf renders: templates/owners/createOrUpdateOwnerForm.html
```

### 11.2 Shared Database Schema

All modules access same database:
```
OWNERS table
  ↓
PETS table (owner_id FK)
  ↓
VISITS table (pet_id FK)
  ↓
VETS table (separate from owner/pet data)
  ↓
VET_SPECIALTIES table
```

No data isolation between modules.

### 11.3 Single Deployment Unit

```
spring-petclinic.jar contains:
├── All Java classes
├── All templates
├── All static resources
├── All configuration
└── Embedded Tomcat

→ Single JAR = Deploy everything or nothing
```

### 11.4 Feature Interdependencies

```
OwnerModule:
  ├─ Depends on: PetController, VisitController
  └─ Shares: Database schema, validation framework, caching

VetModule:
  ├─ Depends on: Spring Data, Caching
  └─ Shares: Database instance, validation framework
```

---

## 12. SCALABILITY LIMITATIONS

### 12.1 Database Bottlenecks

❌ **Single Database Instance**
- All reads and writes to one server
- No read replicas
- Cannot scale reads independently

❌ **EAGER Fetching with Cascade**
```java
@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
```
- Loading owner → loads all pets → loads all visits
- N+1 query potential
- Large result sets for simple queries

### 12.2 Deployment Constraints

❌ **Full Application Redeploy**
- Change in one module → redeploy everything
- Downtime impacts all features
- Blue-green deployment difficult

❌ **Horizontal Scaling Challenges**
- Multiple instances share same database
- Session affinity issues
- No independent feature scaling

### 12.3 Technology Lock-in

❌ **Single Technology Stack**
- Cannot adopt specialized technologies per feature
- One framework for all requirements
- Difficult to integrate new libraries

❌ **Monolithic Codebase**
- Increased complexity as features grow
- Difficult to isolate changes
- Testing entire system for one change

### 12.4 Performance Limits

- Memory constraints (single JVM)
- Garbage collection pauses
- Thread pool limits
- Connection pool exhaustion risk

---

## 13. ARCHITECTURAL PAIN POINTS

### 13.1 Current Issues

| Pain Point | Impact | Severity |
|-----------|--------|----------|
| No authentication/authorization | Security vulnerability | High |
| Single database instance | Scalability bottleneck | High |
| Tight UI-backend coupling | Difficult frontend refactoring | Medium |
| Limited search capabilities | User experience limited | Medium |
| No API versioning | Breaking changes risk | Medium |
| Ad-hoc visit scheduling | No true appointment system | Low |
| No audit logging | Compliance issues | Medium |
| Monolithic tests | Slow test execution | Low |

### 13.2 Feature Limitations

- No user roles/permissions
- No multi-tenancy
- No file upload support
- No email notifications
- No real-time features
- Limited reporting
- No mobile API optimization

---

## 14. MIGRATION READINESS

### 14.1 Potential Service Boundaries

```
Current Monolith → Future Microservices

Owner Service
├── Owner CRUD
├── Owner Search
└── Owns: owners, pets (as aggregate root)

Vet Service
├── Vet Directory
├── Specialty Management
└── Owns: vets, specialties

Visit Service (or Appointment Service)
├── Visit Scheduling
├── Medical Records
└── Depends on: Owner Service, Vet Service APIs

Pet Service (or Care Service)
├── Pet Registry
├── Pet Health Records
└── Depends on: Owner Service API
```

### 14.2 Data Ownership Per Service

| Service | Owns | References |
|---------|------|------------|
| Owner | owners, pets (aggregate) | N/A |
| Vet | vets, specialties | N/A |
| Visit | visits (aggregate) | owner_id (ref), pet_id (ref), vet_id (ref) |

### 14.3 Communication Patterns

**Current:** Direct method calls  
**Future Options:**
- REST APIs
- gRPC
- Event-driven (RabbitMQ, Kafka)
- API Gateway pattern

### 14.4 Phased Approach

**Phase 1:** Extract Visit/Appointment Service
- High cohesion, low coupling
- Minimal dependencies

**Phase 2:** Extract Vet Service
- Self-contained domain
- Limited cross-service calls

**Phase 3:** Shared Services
- Owner/Pet aggregates
- Core business logic

---

## SUMMARY TABLE

| Aspect | Current State | Status |
|--------|---------------|--------|
| **Architecture** | Monolithic MVC | ❌ Tightly coupled |
| **Database** | Single shared instance | ❌ Bottleneck |
| **Deployment** | Single JAR | ❌ All-or-nothing |
| **Scalability** | Vertical only | ❌ Limited |
| **Technology** | Java + Spring | ⚠️ Single stack |
| **Teams** | Must coordinate | ❌ Slow iteration |
| **APIs** | Partial REST | ⚠️ Mixed patterns |
| **Testing** | Integration heavy | ⚠️ Slow suites |
| **Features** | Full-featured | ✅ Complete |
| **Code Quality** | Good patterns | ✅ Spring best practices |

---

## NEXT STEPS

1. ✅ **Complete:** AS-IS Architecture Analysis
2. 📋 **TODO:** Design TO-BE Microservices Architecture
3. 📋 **TODO:** Create Migration Strategy
4. 📋 **TODO:** Build deployment pipelines
5. 📋 **TODO:** Implement service decomposition
6. 📋 **TODO:** Establish API contracts
7. 📋 **TODO:** Configure service mesh (optional)

---

**Generated By:** AI Architecture Analysis Agent  
**Tool:** GitHub Copilot with Subagent Framework  
**Methodology:** Comprehensive code analysis and architectural assessment
