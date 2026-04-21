# Pets & Vets Platform Modernization Guidelines

## Project Context

This workspace contains a legacy monolithic veterinary application (Spring PetClinic) being modernized to support multiple clinics with scalable, cloud-ready infrastructure. The system currently manages customers, pets, and appointments for a single clinic.

**Goal**: Transform into a scalable, multi-clinic platform supporting veterinarians working across multiple locations.

---

## 1. Architecture Principles

### Architectural Pattern
- **Primary**: Microservices architecture with clear bounded contexts
- **Fallback**: Modular monolith only if complexity requires phasing
- **Approach**: Domain-Driven Design (DDD) to structure services around business capabilities

### Key Requirements
- **Multi-clinic support**: Systems must account for multiple clinic locations
- **Scalability**: Each service independently deployable and scalable
- **Cloud-ready**: Designed for AWS, Azure, or GCP (generic principles preferred)
- **Headless architecture**: Backend APIs independent of frontend delivery
- **Backward compatibility**: Migration uses Strangler Pattern to avoid cutover risks

### Enforcement
- All architecture diagrams must show service boundaries clearly
- Every service decision must justify multi-clinic support
- Documentation must map requirements → architectural decisions

---

## 2. Core Services (Always Consider)

These five services form the foundation. Every feature must identify which service owns it:

| Service | Responsibilities |
|---------|------------------|
| **User Service** | Authentication, authorization, profiles (vets, customers, admin staff) |
| **Clinic Service** | Clinic locations, capacity, operating hours, staff assignments |
| **Pet Service** | Pet registry, health records, attributes, medical history |
| **Appointment Service** | Scheduling, availability, confirmation, status tracking |
| **Notification Service** | Customer alerts, vaccination reminders, appointment confirmations (async) |

**Rule**: If a feature doesn't fit these five services, propose a new service with business justification.

---

## 3. Data Strategy

### Database Pattern
- **Per-service model**: Each microservice owns its database (no shared schemas)
- **Relational DB**: PostgreSQL or MySQL for structured entities (owners, pets, appointments)
- **NoSQL option**: Document stores (MongoDB) for flexible data like pet health records, medical notes
- **Event sourcing**: Consider for audit trails (appointment changes, notification history)

### Shared Data Handling
- **Centralized registry**: User Service maintains master list of vets/customers
- **Service subscribers**: Replicate only necessary data (e.g., Appointment Service subscribes to Pet.created events)
- **Avoid joins across services**: Use eventual consistency or ID references

### Data Consistency
- Prefer eventual consistency over distributed transactions
- Use saga pattern for multi-service workflows (e.g., appointment booking → notification)

---

## 4. Communication Patterns

### Synchronous: REST API
```
[API Gateway] → [Service A] → [Service B]
Use when: Immediate response needed, request-response semantics
Standards: RESTful design, /api/v1/... versioning, 10-second timeout max
```

### Asynchronous: Event-Driven
```
[Service A] publishes → [Message Broker] → [Service B] subscribes
Use when: Notification, eventual consistency, decoupling required
Broker: Kafka (recommended) or RabbitMQ
Example: AppointmentCreated → Notification Service sends SMS/email
```

### Hybrid Pattern
- **Appointment creation**: Sync REST to Appointment Service, async events for notifications
- **Vet availability update**: Event to Appointment Service, no blocking

### No Service Calls During Event Processing
- Events are one-way broadcasts; never call back to origin service in handlers

---

## 5. API Standards

### URL Versioning
```
/api/v1/clinics/{clinicId}/appointments
/api/v1/pets/{petId}/health-records
/api/v1/users/{userId}/profile
```

### Request/Response
- Model validation (input shape, required fields) at service boundary
- HTTP error codes: 400 (validation), 401 (auth), 403 (permission), 404 (not found), 500 (server error)
- All responses include consistent metadata:
  ```json
  {
    "success": true,
    "data": { /* payload */ },
    "timestamp": "2026-04-21T10:30:00Z",
    "requestId": "req-abc123"
  }
  ```

### Authentication & Authorization
- **JWT tokens**: Stateless, signed at User Service, validated at API Gateway
- **Roles**: Admin, Vet, Customer (role-based access control)
- **Claim Structure**:
  ```json
  {
    "sub": "user-id",
    "role": "vet",
    "clinicIds": ["clinic-1", "clinic-2"],
    "exp": 1234567890
  }
  ```

---

## 6. Cloud & Deployment Strategy

### Architecture Components
```
[Load Balancer] 
    ↓
[API Gateway] ← Rate limiting, auth validation, routing
    ↓
[Service Mesh] ← Observability, resilience
    ↓
[Microservices] (containerized)
    ↓
[Databases] (per-service)
    ↓
[Message Broker] (Kafka/RabbitMQ)
```

### Containerization & Orchestration
- **Containers**: Docker (multi-stage builds for size optimization)
- **Orchestration**: Kubernetes (EKS on AWS, AKS on Azure, GKE on Google Cloud)
- **Service Mesh**: Optional but recommended (Istio for observability, circuit breaking)

### Storage
- **Databases**: Managed services (RDS, Azure Database, Cloud SQL)
- **Object storage**: S3 (AWS) for pet images, medical records (PDFs)
- **Cache**: Redis for session management, Vet availability caching

### Monitoring & Logging
- Centralized logging (ELK Stack, CloudWatch, or DataDog)
- Distributed tracing (Jaeger, Zipkin)
- Alerts on error rates > 1%, latency > 500ms, database connections > 80%

---

## 7. Security

### Authentication Flow
1. Customer/Vet logs in via User Service → JWT issued
2. Each request includes JWT in `Authorization: Bearer <token>` header
3. API Gateway validates signature, extracts claims
4. Service reads `clinicIds` claim → authorizes clinic access

### Multi-Tenancy Isolation
- No customer can query data outside their assigned clinic
- Queries always filter by `clinicId` at database layer
- No cross-clinic data visibility in cache/logs

### Best Practices
- All APIs use HTTPS/TLS
- API keys for service-to-service communication (short-lived, rotated monthly)
- Secrets management (AWS Secrets Manager, HashiCorp Vault)
- PII encryption at rest (pet names, owner phone numbers)

---

## 8. Documentation Standards

### Always Produce Structured Markdown

**Mandatory Sections** (in order):
1. **Problem Understanding**: What limitation/gap are we addressing?
2. **Proposed Solution**: High-level approach
3. **Architecture Design**: Diagrams (ASCII or Mermaid), component flow
4. **Services Breakdown**: Table with service name, responsibility, tech stack
5. **Data & API Design**: Schema overview, key REST endpoints, event definitions
6. **Deployment Strategy**: Infrastructure, deployment pipeline, scaling policy
7. **Trade-offs**: Complexity, cost, performance implications
8. **Migration Plan**: Step-by-step transition from current to target state

### Visual Aids
- Use **Mermaid diagrams** for:
  - Service dependency graphs
  - Event flow sequences
  - Database relationships
- Use **ASCII tables** for service inventory, endpoint routes
- Use **code blocks** for API examples, schema definitions

### Code Examples
- Always include runnable examples (cURL, gRPC definitions, event payloads)
- Every example must show multi-clinic context (e.g., `clinicId` parameter)

---

## 9. Migration Strategy: Strangler Pattern

### Phase Model
```
[Monolith] + [New Services] → [New Services Only]
```

### Execution
1. **Phase 1**: Deploy API Gateway in front of monolith
2. **Phase 2**: Extract User Service; gateway routes `/api/users/*` → User Service
3. **Phase 3**: Extract Pet Service; gradual traffic shift
4. **Phase 4**: Extract Appointment Service; remaining business logic
5. **Phase 5**: Retire monolith after validation

### Rollback Safety
- Every phase is independently deployable
- Monolith remains operational during extraction
- A/B testing on new services before full cutover
- Automatic rollback if error rate exceeds 5%

---

## 10. Conventions

### Naming
- **Services**: kebab-case (`user-service`, `clinic-service`, `pet-service`)
- **Databases**: `{service}_{environment}` (`pet_prod`, `appointment_staging`)
- **Kafka topics**: `{service}.{event}` (`appointment.created`, `pet.health_record_updated`)
- **Tables**: snake_case, plural form (`appointments`, `pet_health_records`)

### Branching Strategy
- `main`: Production-ready code
- `develop`: Integration branch for services
- `feature/{service}/{description}`: New feature branches
- `fix/{service}/{issue}`: Bug fix branches

### Commit Messages
```
[service-name] Verb: description

[user-service] Add: JWT validation middleware
[pet-service] Fix: Handle null birthDate in pet creation
[appointment-service] Refactor: Extract availability logic
```

### Code Reviews
- Every merge requires approval from one architect or tech lead
- Cross-service changes require coordination
- Security review mandatory for auth/multi-tenancy changes

---

## 11. Output Checklist

When delivering architecture, implementation, or analysis:

- [ ] Problem clearly stated with business justification
- [ ] Each service's responsibility explicitly defined
- [ ] Multi-clinic support demonstrated or explained
- [ ] Data flow shown (requests, events, responses)
- [ ] Deployment architecture specified (containers, orchestration, databases)
- [ ] Security considerations addressed (auth, isolation, encryption)
- [ ] Trade-offs discussed (complexity vs. benefit)
- [ ] Migration path outlined (how to transition from current state)
- [ ] Markdown properly formatted with headings, code blocks, tables
- [ ] Mermaid diagrams or ASCII art provided where beneficial
- [ ] Runnable examples included (API calls, event payloads)

---

## 12. Technology Stack (Reference)

| Layer | Current | Target |
|-------|---------|--------|
| **Framework** | Spring Boot 3.3 | Spring Boot 3.x (continued) |
| **Language** | Java 17 | Java 17+ |
| **API Gateway** | None | Kong, AWS API Gateway, or NGINX |
| **Service Mesh** | None | Istio (optional) |
| **Database** | H2/MySQL/PostgreSQL | PostgreSQL per service + MongoDB (optional) |
| **Message Broker** | None | Kafka or RabbitMQ |
| **Caching** | Caffeine (in-process) | Redis (distributed) |
| **Containers** | None | Docker |
| **Orchestration** | None | Kubernetes (EKS/AKS/GKE) |
| **Logging** | System logs | ELK Stack / CloudWatch |
| **Tracing** | None | Jaeger / Zipkin |
| **Frontend** | Thymeleaf (monolith) | React/Vue (headless, independent) |

---

## 13. Key Decision Points

When proposing features or changes, address:

1. **Which service owns this?** (Is it User, Clinic, Pet, Appointment, Notification, or new?)
2. **What data does it need?** (From which databases?)
3. **How do services communicate?** (Sync REST or async events?)
4. **Is it multi-clinic aware?** (Does it work across clinic boundaries correctly?)
5. **How does it scale?** (Single instance, replicas, caching strategy?)
6. **What are failure modes?** (What happens if this service is down?)

---

## Questions or Ambiguities?

When in doubt, prioritize:
1. **Production readiness** over proof-of-concept
2. **Scalability** over feature count
3. **Operational simplicity** over architectural purity
4. **Documentation** over code elegance
