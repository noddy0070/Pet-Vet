# AS-IS Architecture Summary
## Spring PetClinic - Quick Reference

---

## 📊 Architecture at a Glance

```
Single Spring Boot JAR
    ↓
┌─────────────────────────────────────┐
│  Controllers                        │
│  ├─ OwnerController                 │
│  ├─ PetController                   │
│  ├─ VisitController                 │
│  ├─ VetController                   │
│  └─ OwnerRestController             │
└────────────┬────────────────────────┘
             ↓
┌─────────────────────────────────────┐
│  Repositories (Spring Data JPA)     │
│  ├─ OwnerRepository                 │
│  └─ VetRepository                   │
└────────────┬────────────────────────┘
             ↓
┌─────────────────────────────────────┐
│  Single Database Instance           │
│  ├─ H2 (Default)                    │
│  ├─ MySQL 8.4+                      │
│  └─ PostgreSQL 16.3+                │
└─────────────────────────────────────┘
```

---

## 🎯 Key Metrics

| Metric | Value |
|--------|-------|
| **Architecture** | Monolithic MVC |
| **Technology** | Java 17+ / Spring Boot 3.3.2 |
| **Database** | Single shared instance |
| **Modules** | 4 (Owner, Vet, System, Model) |
| **Controllers** | 5 |
| **Repositories** | 2 |
| **Entities** | 7 |
| **Tables** | 7 |
| **Sample Data** | 10 owners, 6 vets, 13 pets |
| **Endpoints** | 12+ (HTML + REST) |
| **Languages** | 4 (EN, DE, ES, KO) |

---

## 🔄 Data Flow Summary

### View Owner Details: `GET /owners/1`

```
Browser Request
    ↓
Spring DispatcherServlet
    ↓
OwnerController.showOwner(1)
    ↓
OwnerRepository.findById(1)
    ↓
Hibernate Query: SELECT owner, pets (FETCH JOIN)
    ↓
Database Query: SELECT o.*, p.* FROM owners o LEFT JOIN pets p WHERE o.id=1
    ↓
Entity Mapping: Owner(id=1, pets=[Pet{...}])
    ↓
Thymeleaf Rendering: ownerDetails.html
    ↓
HTML Response
    ↓
Browser Renders Page
```

### Search Owners: `GET /owners?page=1&lastName=D`

```
Browser Request
    ↓
OwnerController.processFindForm(1, owner)
    ↓
OwnerRepository.findByLastName("D", pageable)
    ↓
Query: WHERE owner.lastName LIKE 'D%'
    ↓
Results: Betty Davis, Harold Davis (2 owners)
    ↓
Paginated Response (page 1, pageSize 5)
    ↓
Thymeleaf: ownersList.html
    ↓
HTML Table with pagination controls
```

### Create New Owner: `POST /owners/new`

```
Form Submission
    ↓
OwnerController.processCreationForm(owner)
    ↓
Validation (@NotBlank, @Pattern)
    ↓
If errors → Return form with messages
If valid:
    ↓
OwnerRepository.save(owner)
    ↓
INSERT into owners table
    ↓
Redirect to /owners/{newOwnerId}
```

---

## 🗂️ Module Structure

### Owner Module
- **Entities:** Owner, Pet, PetType, Visit
- **Controllers:** OwnerController, PetController, VisitController
- **Repository:** OwnerRepository
- **Endpoints:** 7 (3 for owner, 2 for pet, 2 for visit)
- **Database:** owners, pets, visits, types tables

### Vet Module
- **Entities:** Vet, Specialty
- **Controllers:** VetController, OwnerRestController
- **Repository:** VetRepository (@Cacheable)
- **Endpoints:** 2 (HTML + JSON)
- **Database:** vets, specialties, vet_specialties tables

### System Module
- **Configuration:** CacheConfiguration (Caffeine)
- **Controllers:** WelcomeController, CrashController
- **Purpose:** App-level services & error handling

---

## 📋 Endpoint Quick Reference

| Method | Path | Handler | Returns |
|--------|------|---------|---------|
| GET | `/` | WelcomeController | HTML |
| GET | `/owners/new` | OwnerController.initCreation | HTML Form |
| POST | `/owners/new` | OwnerController.processCreation | Redirect |
| GET | `/owners/find` | OwnerController.initFind | HTML Form |
| GET | `/owners?page=1&lastName=X` | OwnerController.processFind | HTML List |
| GET | `/owners/{id}` | OwnerController.showOwner | HTML Details |
| GET | `/owners/{id}/edit` | OwnerController.initUpdate | HTML Form |
| POST | `/owners/{id}/edit` | OwnerController.processUpdate | Redirect |
| GET | `/owners/{ownerId}/pets/new` | PetController.initCreation | HTML Form |
| POST | `/owners/{ownerId}/pets/new` | PetController.processCreation | Redirect |
| GET | `/vets.html` | VetController.showVetList | HTML |
| GET | `/vets` | VetController.showResourcesList | JSON |
| GET | `/api/owners/find?page=1&lastName=X` | OwnerRestController.findOwners | JSON |

---

## 💾 Database Schema

```
OWNERS (10 rows)
├─ id, first_name, last_name, address, city, telephone
│
PETS (13 rows) ─← owner_id FK
├─ id, name, birth_date, type_id FK, owner_id FK
│
├─ VISITS (4 rows) ─← pet_id FK
│  └─ id, pet_date, description
│
└─ TYPES (6 rows) ─← type_id FK
   └─ id, name (cat, dog, etc.)

VETS (6 rows)
├─ id, first_name, last_name
│
└─ VET_SPECIALTIES (join table, 5 rows)
   └─ vet_id FK, specialty_id FK
      │
      └─ SPECIALTIES (3 rows)
         └─ id, name (radiology, surgery, dentistry)
```

---

## 🔐 Validation Rules

| Entity | Field | Rule |
|--------|-------|------|
| **Owner** | address | @NotBlank |
| | city | @NotBlank |
| | telephone | @NotBlank, @Pattern(\\d{10}) |
| **Pet** | name | Required, unique per owner |
| | birth_date | Cannot be future date |
| **Visit** | description | @NotBlank |

---

## ⚙️ Configuration

### Active Profile Selection
```bash
# Default (H2)
java -jar spring-petclinic.jar

# MySQL
java -jar spring-petclinic.jar --spring.profiles.active=mysql

# PostgreSQL
java -jar spring-petclinic.jar --spring.profiles.active=postgres
```

### Key Properties
```properties
database=h2
spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=true
spring.thymeleaf.mode=HTML
spring.messages.basename=messages/messages
management.endpoints.web.exposure.include=*
```

### Caching
- **Type:** Caffeine
- **Strategy:** @Cacheable on VetRepository
- **Cache Name:** "vets"
- **TTL:** 10 minutes
- **Max Size:** 1000 entries

---

## 🚀 Monolithic Characteristics

### ✅ Strengths
- Simple to understand and develop
- Single technology stack (consistent)
- Direct database access (no network latency)
- ACID transactions across modules
- Quick to prototype

### ❌ Weaknesses
- **Scalability:** Single database bottleneck
- **Deployment:** All-or-nothing redeployment
- **Coupling:** Controllers → Repositories → DB (tight)
- **Team Coordination:** All teams modify same codebase
- **Technology Lock-in:** Single Java/Spring stack
- **Flexibility:** Cannot adopt specialized tech per feature

---

## 📈 Performance Characteristics

| Aspect | Impact |
|--------|--------|
| **EAGER Fetching** | Owner → loads all pets → loads all visits (potential N+1) |
| **Database Connection** | Single pool, shared across all modules |
| **Caching** | Vets cached, but owners/pets not cached |
| **Pagination** | Only on some endpoints (5 items per page) |
| **Transaction Scope** | Application-wide, single database |

---

## 🎬 Current Limitations

| Limitation | Impact |
|-----------|--------|
| No authentication/authorization | Anyone can access any owner/pet data |
| No appointment scheduling | Visits created ad-hoc, not scheduled |
| No user roles | Single access level for all users |
| No multi-tenancy | Single tenant, single database |
| No API versioning | Breaking changes affect all clients |
| No audit logging | No change history |
| Limited search | Last name only, no advanced filtering |
| No email notifications | No automated communications |

---

## 🔄 Request Lifecycle

```
1. HTTP Request arrives at Tomcat
2. Spring DispatcherServlet intercepts
3. Routing: URL → Handler Method (@RequestMapping)
4. Parameter extraction: URL, query, body
5. @ModelAttribute binding: Objects from request
6. Validation: @Valid + custom validators
7. Business logic: Repository calls + DB operations
8. Model assembly: Add data to model
9. View selection: Return view name
10. Template rendering: Thymeleaf processes template
11. HTML generation: Populate template with model data
12. HTTP response: 200 OK + HTML
13. Browser rendering: Display page
```

---

## 📊 Entity Relationships

```
Owner
├─ OneToMany → Pets (cascade: ALL, fetch: EAGER)
   └─ ManyToOne → PetType
   └─ OneToMany → Visits (cascade: ALL, fetch: EAGER)

Vet
└─ ManyToMany → Specialties (via VET_SPECIALTIES join table)
```

**Fetch Strategy:**
- Owner relationships: EAGER (always load pets)
- Pet relationships: EAGER (always load visits)
- Vet relationships: Lazy (load on demand)

---

## 🔗 Dependencies

### Explicit
- `OwnerController` → `OwnerRepository`
- `PetController` → `OwnerRepository`
- `VisitController` → `OwnerRepository`
- `VetController` → `VetRepository`
- `OwnerRestController` → `OwnerRepository`

### Implicit
- All modules → Database (single instance)
- All controllers → Spring container
- All repositories → JPA/Hibernate
- Templates → Model data from controllers

---

## 🎯 Service Boundary Candidates (Future)

### Service 1: Owner Service ✅ (High Cohesion)
- Owns: Owner aggregate (Owner + Pet + Visit)
- APIs: CRUD owner, search, manage pets
- Independent: Minimal external dependencies

### Service 2: Vet Service ✅ (High Cohesion)
- Owns: Vet aggregate (Vet + Specialty)
- APIs: List vets, manage specialties
- Independent: Self-contained domain

### Service 3: Appointment Service ⚠️ (Dependent)
- Owns: Appointment/Visit aggregate
- APIs: Schedule, manage appointments
- Dependencies: Owner Service API, Vet Service API

---

## 📝 Next Steps

1. ✅ **COMPLETE:** AS-IS Architecture Analysis
2. ⏭️ **NEXT:** Design TO-BE Microservices Architecture
3. ⏭️ **THEN:** Create Migration Strategy
4. ⏭️ **THEN:** Implement Service Decomposition
5. ⏭️ **THEN:** Build Deployment Pipelines

---

## 📚 Related Documents

- [AS-IS_DETAILED_ANALYSIS.md](AS-IS_DETAILED_ANALYSIS.md) - Comprehensive analysis with code examples
- [AS-IS_ARCHITECTURE.md](AS-IS_ARCHITECTURE.md) - High-level architecture overview
- [PROJECT_ANALYSIS.md](PROJECT_ANALYSIS.md) - Full project assessment

---

**Status:** ✅ Complete  
**Generated:** April 21, 2026  
**Agent:** AI Architecture Analysis Framework  
**Method:** Comprehensive codebase analysis with architectural patterns identification
