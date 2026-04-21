# Pets & Vets Platform: Modernization Architecture Document

**Version**: 1.0  
**Date**: April 21, 2026  
**Status**: Strategic Architecture Design  
**Audience**: Technical Leadership, System Architects, Development Teams

---

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [As-Is Architecture](#as-is-architecture)
3. [Problems & Limitations](#problems--limitations)
4. [Gap Analysis](#gap-analysis)
5. [To-Be Architecture Design](#to-be-architecture-design)
6. [Microservices Breakdown](#microservices-breakdown)
7. [Data Model Overview](#data-model-overview)
8. [API & Communication Design](#api--communication-design)
9. [Cloud & Deployment Strategy](#cloud--deployment-strategy)
10. [Security & Multi-Tenancy](#security--multi-tenancy)
11. [Migration Plan](#migration-plan)
12. [Trade-offs & Considerations](#trade-offs--considerations)
13. [Final Summary & Recommendations](#final-summary--recommendations)

---

## Executive Summary

The current Pets & Vets application is a **monolithic Spring Boot application** designed for a single veterinary clinic. While well-structured, it cannot support multiple clinics, lacks scalability for growth, and has no service-oriented architecture for distributed evolution.

**Modernization Objective**: Transform into a **cloud-native, microservices-based platform** supporting:
- Multiple clinics and veterinarians across locations
- Independent service scaling and deployment
- Event-driven notifications and features
- Multi-tenant data isolation
- Headless architecture for flexible frontend delivery

**Timeline**: 12-18 months (6-month phases using Strangler Pattern)  
**Investment**: Infrastructure modernization + team skill development  
**Expected ROI**: 40% faster feature delivery, 60% cost reduction through cloud scaling, new market segments (SaaS play)

---

## As-Is Architecture

### Current System Overview

```
┌─────────────────────────────────────────────────┐
│         SPRING BOOT MONOLITH                    │
│  (Single Deployment, Shared Database)           │
├─────────────────────────────────────────────────┤
│  Frontend Layer (Thymeleaf + Bootstrap)         │
│  ├─ HTML Templates (MVC views)                  │
│  └─ CSS/JS Resources                            │
├─────────────────────────────────────────────────┤
│  Controller Layer (MVC + REST)                  │
│  ├─ OwnerController (/owners, /owners/{id})     │
│  ├─ PetController (/owners/{id}/pets/...)       │
│  ├─ VisitController (appointments)              │
│  ├─ VetController (/vets)                       │
│  └─ OwnerRestController (/api/owners/find)      │
├─────────────────────────────────────────────────┤
│  Service Layer (Business Logic)                 │
│  ├─ Repository Pattern (JPA/Hibernate)          │
│  ├─ OwnerRepository (custom @Query methods)     │
│  ├─ VetRepository (@Cacheable("vets"))          │
│  └─ Transaction Management (@Transactional)     │
├─────────────────────────────────────────────────┤
│  Persistence Layer                              │
│  ├─ Spring Data JPA + Hibernate ORM             │
│  ├─ Caffeine In-Process Caching                 │
│  └─ EAGER fetching + Cascade ALL                │
├─────────────────────────────────────────────────┤
│  Shared Database (H2 / MySQL / PostgreSQL)      │
│  ├─ vets, specialties, vet_specialties          │
│  ├─ owners, pets, types                         │
│  └─ visits (appointments)                       │
└─────────────────────────────────────────────────┘
```

### Current Data Model

```
BaseEntity (ID generation)
├─ Person
│  ├─ Owner (first_name, last_name, address, city, telephone)
│  │  └─ pets: List<Pet> [1-to-many, EAGER, CASCADE ALL]
│  └─ Vet (first_name, last_name)
│     └─ specialties: Set<Specialty> [many-to-many, EAGER]
├─ NamedEntity
│  ├─ Pet (name, type_id, birth_date, owner_id)
│  │  └─ visits: Set<Visit> [1-to-many, EAGER, ordered by visit_date]
│  ├─ PetType (name)
│  └─ Specialty (name)
└─ Visit (date, description, pet_id)
```

### Current Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.3.2 |
| Language | Java | 17 |
| ORM | Spring Data JPA + Hibernate | Latest |
| Database | H2 (dev), MySQL, PostgreSQL | - |
| Templating | Thymeleaf | 3.x |
| Frontend CSS | Bootstrap | 5.3.3 |
| Caching | Caffeine | In-process, in-memory |
| Build Tools | Maven + Gradle | - |
| Testing | Spring Boot Test, Testcontainers | - |

### Key Characteristics

✅ **Monolithic Design**
- Single Spring Boot JAR deployment
- All features in one process
- Shared schema, centralized data

✅ **Eager Data Loading**
- All Owner relationships fetched immediately (Owner → Pets → Visits)
- Potential N+1 query problems at scale
- Suitable for small dataset but problematic with large clinics

✅ **In-Process Caching**
- Caffeine cache for vets list (@Cacheable)
- No distributed caching across instances
- Loss on deploy/restart

✅ **Coupled Frontend & Backend**
- Thymeleaf templates rendered server-side
- Frontend changes require backend redeployment
- Limited mobile/SPA support

✅ **Single Deployment Unit**
- All features deploy together
- Feature changes risk entire system
- Scaling difficult (must scale all features equally)

---

## Problems & Limitations

### 1. Multi-Clinic Support (CRITICAL)

**Current Problem**:
- No clinic entity or concept
- All data treated as single monolith
- Vets and owners have no clinic assignment
- Impossible to segregate data by location

**Impact**: Cannot serve franchise models, expansion to multiple locations blocked, regulatory compliance issues (data isolation).

### 2. Scalability Constraints

**Current Problem**:
- Monolithic architecture requires full system replication
- Appointment scheduling (high-read) scaled same as user admin (low-volume)
- Inefficient resource utilization
- Caffeine caching not distributed (only per-instance)

**Impact**: Becomes expensive at scale, poor responsiveness under load.

### 3. Tight Coupling

**Current Problem**:
- Frontend (Thymeleaf) tightly bound to backend
- Changing UI requires backend build/deploy
- Cannot support mobile apps independently
- REST endpoints are secondary, MVC-first design

**Impact**: Slow feature delivery, high deployment risk.

### 4. Data Consistency & EAGER Loading

**Current Problem**:
- ALL relationships fetched eagerly (Owner → Pets → Visits)
- Potential for massive query overhead with large clinics
- Cascade ALL on deletes (risky, no granular control)
- No event-driven updates (notifications computed synchronously)

**Impact**: Performance degradation with large datasets, inefficient memory usage.

### 5. No Asynchronous Processing

**Current Problem**:
- Notifications would block appointment requests
- Vaccination reminders must be computed inline
- Scaling notification volume difficult
- No audit trail of state changes

**Impact**: User-facing latency, operational inflexibility.

### 6. Limited Data Model

**Current Problem**:
- No clinic entity
- No health records or medical history beyond visits
- No pets image/document storage
- No audit/versioning of changes

**Impact**: Cannot track detailed pet health, manage multi-location workflows.

### 7. Single Deployment & Release Risk

**Current Problem**:
- Any bug in user module affects appointment scheduling
- Features ship together regardless of readiness
- Difficult to do blue-green deployments
- High blast radius for changes

**Impact**: Change management and risk mitigation difficult, slow release cycle.

### 8. Limited Observability

**Current Problem**:
- No distributed tracing
- Logs all mixed in single process
- Difficult to diagnose cross-service issues (once refactored)
- No metric isolation per business capability

**Impact**: Operational troubleshooting slow and error-prone.

---

## Gap Analysis

| Capability | As-Is | To-Be | Gap | Priority |
|-----------|-------|-------|-----|----------|
| **Multi-Clinic Support** | ❌ None | ✅ Clinic entity, data isolation | Design clinic hierarchy, implement RBAC | CRITICAL |
| **Microservices** | ❌ Monolith | ✅ User, Clinic, Pet, Appointment, Notification services | Architecture, service extraction, database split | CRITICAL |
| **Headless API** | ⚠️ Secondary REST endpoints | ✅ Primary REST/GraphQL focus | Refactor to API-first design, SDK generation | HIGH |
| **Event-Driven Architecture** | ❌ None | ✅ Kafka/RabbitMQ, event streaming | Message broker setup, event schemas, saga patterns | HIGH |
| **Distributed Caching** | ⚠️ In-process Caffeine | ✅ Redis distributed cache | Infrastructure, client libraries, cache invalidation | MEDIUM |
| **Cloud Deployment** | ❌ On-premise only | ✅ AWS/Azure/GCP ready | Docker, Kubernetes, managed services setup | HIGH |
| **Multi-Tenancy Isolation** | ❌ None | ✅ Data isolation by clinic, RBAC enforcement | Database schema isolation, query filters, audit logging | CRITICAL |
| **Audit & Versioning** | ❌ None | ✅ Change tracking, history | Event sourcing or audit tables, temporal data | MEDIUM |
| **Notification System** | ❌ Synchronous | ✅ Async via Notification Service | Service design, template engine, channel integration (SMS, email) | HIGH |
| **Distributed Tracing** | ❌ None | ✅ Jaeger/Zipkin integration | Instrumentation, correlation IDs, visualization | MEDIUM |
| **API Gateway** | ❌ None | ✅ Kong/AWS API Gateway | Rate limiting, auth validation, routing, secrets | HIGH |
| **Service Mesh** | ❌ None | ✅ Istio (optional) | Circuit breaking, retries, service-to-service auth | LOW |
| **Pet Health Records** | ⚠️ Visit notes only | ✅ Documents, images, medical history | Document storage (S3), metadata schema | MEDIUM |
| **Database per Service** | ❌ Shared schema | ✅ Isolated databases | Schema design per service, data replication strategy | HIGH |
| **Documentation** | ⚠️ Basic README | ✅ API docs, architecture, runbooks | OpenAPI specs, ADRs, deployment playbooks | HIGH |

---

## To-Be Architecture Design

### High-Level System Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                        CDN / Static Assets                     │
│                      (S3 + CloudFront)                         │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│                    LOAD BALANCER (ALB)                         │
│              (Auto-scaling, health checks)                     │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│           API GATEWAY (JWT Validation, Rate Limiting)          │
│                                                                │
│  Routes:                                                       │
│  ├─ /api/v1/users/* → User Service                            │
│  ├─ /api/v1/clinics/* → Clinic Service                        │
│  ├─ /api/v1/pets/* → Pet Service                              │
│  ├─ /api/v1/appointments/* → Appointment Service              │
│  └─ /health, /metrics → Observability                         │
└────────────────────────────────────────────────────────────────┘
                              ↓
                    ┌─────────┬─────────────┬──────────┐
                    ↓         ↓             ↓          ↓
            ┌──────────┐  ┌──────────┐  ┌──────┐  ┌──────────┐
            │ User Svc │  │Clinic Svc│  │Pet   │  │Appt. Svc │
            └────┬─────┘  └────┬─────┘  │Svc   │  └────┬─────┘
                 │             │        └──┬───┘       │
                 │             │           │           │
         ┌───────↓─────────────↓───────────↓───────────↓───────┐
         │     SERVICE MESH (Istio - Optional)                │
         │  Circuit Breakers, Retries, Service Auth          │
         └───────────────────────────────────────────────────┘
                              ↓
         ┌────────────────────┴──────────────────┐
         ↓                                        ↓
    ┌─────────────────────┐          ┌──────────────────────────┐
    │  PostgreSQL DB      │          │  Message Broker (Kafka)  │
    │  (user_service_db)  │          │                          │
    ├─────────────────────┤          │  Topics:                 │
    │  Users              │          │  ├─ user.registered     │
    │  Roles/Permissions  │          │  ├─ clinic.created      │
    │  Authorization      │          │  ├─ pet.created         │
    └─────────────────────┘          │  ├─ appointment.created  │
                                     │  ├─ vaccination_due     │
         ┌─────────────────────┐     │  └─ reminder.sent       │
         │  PostgreSQL DB      │     └──────────────────────────┘
         │  (clinic_service_db)│              ↓
         ├─────────────────────┤     ┌──────────────────────┐
         │  Clinics            │     │ Notification Service │
         │  Staff Assignments  │     ├──────────────────────┤
         │  Operating Hours    │     │  Email Provider      │
         │  Capacity           │     │  SMS Gateway         │
         └─────────────────────┘     │  Push Notifications  │
                                     └──────────────────────┘
         ┌──────────────────────────┐
         │  PostgreSQL DB           │
         │  (pet_service_db)        │
         ├──────────────────────────┤
         │  Pets                    │
         │  PetTypes                │
         │  Health Records          │
         │  Vaccination History     │
         └──────────────────────────┘
              │
              ↓
         ┌──────────────────────────┐
         │  MongoDB                 │
         │  (pet_health_records_db) │
         ├──────────────────────────┤
         │  Medical Notes (JSON)    │
         │  Document Index          │
         │  Flexible Schemas        │
         └──────────────────────────┘
              │
              ↓
         ┌──────────────────────────┐
         │  S3 / Object Storage     │
         ├──────────────────────────┤
         │  Pet Images              │
         │  Medical Reports         │
         │  Vaccination Records     │
         └──────────────────────────┘

         ┌──────────────────────────┐
         │  PostgreSQL DB           │
         │  (appointment_db)        │
         ├──────────────────────────┤
         │  Appointments            │
         │  Availability Slots      │
         │  Confirmations           │
         └──────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│           OBSERVABILITY STACK                                  │
├────────────────────────────────────────────────────────────────┤
│  Logging: ELK Stack (Elasticsearch, Logstash, Kibana)         │
│  Tracing: Jaeger / Zipkin                                     │
│  Metrics: Prometheus + Grafana                                │
│  Monitoring: CloudWatch / Azure Monitor / Stackdriver         │
└────────────────────────────────────────────────────────────────┘
```

### Architecture Principles

1. **Service Independence**: Each service owns data, logic, API contracts
2. **Async First**: Notifications, reminders, state changes via events
3. **Database per Service**: No shared schemas, service boundaries enforced
4. **API Gateway Pattern**: Centralized entry point for routing, auth, rate limiting
5. **Event Hub Model**: Kafka as single source of truth for domain events
6. **Multi-Tenancy**: Clinic isolation at data layer, RBAC at API layer
7. **Cloud-Native**: Containerized, orchestrated, auto-scaling, managed services

---

## Microservices Breakdown

### 1. User Service

**Responsibility**: Authentication, user profiles, authorization

| Property | Value |
|----------|-------|
| **Port** | 8001 |
| **Database** | PostgreSQL (user_service_db) |
| **Key Tables** | users, roles, permissions, role_assignments |
| **Events Produced** | user.registered, user.role_updated, user.clinic_assigned |
| **Events Consumed** | clinic.created |
| **Cache** | Redis (user profiles, role permissions) |

**API Endpoints**:
```
POST   /api/v1/auth/register          # Customer registration
POST   /api/v1/auth/login             # Login (returns JWT)
POST   /api/v1/auth/refresh           # Refresh token
POST   /api/v1/users                  # Admin creates user
GET    /api/v1/users/{userId}         # Get profile
PUT    /api/v1/users/{userId}         # Update profile
GET    /api/v1/users/{userId}/roles   # List user roles/clinics
POST   /api/v1/users/{userId}/clinics/{clinicId}  # Assign to clinic
```

**Key Entities**:
```java
User
  - id, email, passwordHash, firstName, lastName
  - createdAt, lastLogin, isActive

Role
  - id, name (ADMIN, VET, CUSTOMER, STAFF)
  - permissions: Set<String>

UserRole
  - userId, roleId, clinicId (multi-clinic support)
  - grantedAt, grantedBy
```

**Technology**: Spring Boot, Spring Security, PostgreSQL, Redis

---

### 2. Clinic Service

**Responsibility**: Clinic management, locations, operating hours, capacity

| Property | Value |
|----------|-------|
| **Port** | 8002 |
| **Database** | PostgreSQL (clinic_service_db) |
| **Key Tables** | clinics, operating_hours, staff_assignments, capacity_rules |
| **Events Produced** | clinic.created, clinic.updated, staff.assigned, hours.changed |
| **Events Consumed** | user.registered, appointment.created |
| **Cache** | Redis (clinic hours, capacity status) |

**API Endpoints**:
```
POST   /api/v1/clinics                # Admin creates clinic
GET    /api/v1/clinics                # List clinics (filtered by user role)
GET    /api/v1/clinics/{clinicId}     # Get clinic details
PUT    /api/v1/clinics/{clinicId}     # Update clinic
POST   /api/v1/clinics/{clinicId}/hours    # Set operating hours
GET    /api/v1/clinics/{clinicId}/vets     # List vets at clinic
POST   /api/v1/clinics/{clinicId}/staff    # Assign staff
GET    /api/v1/clinics/{clinicId}/capacity  # Get availability summary
```

**Key Entities**:
```java
Clinic
  - id, name, address, phone, city, state, zipCode
  - maxPetsPerDay, maxAppointmentDurationMins

OperatingHours
  - clinicId, dayOfWeek, openTime, closeTime, isOpen

StaffAssignment
  - clinicId, userId (vet), specialties: Set<String>
  - startDate, endDate, isPrimary

ClinicCapacity
  - clinicId, date, normalCapacity, emergencyCapacity, booked
```

**Technology**: Spring Boot, PostgreSQL, Redis, Event Publisher

---

### 3. Pet Service

**Responsibility**: Pet registry, health records, vaccination tracking

| Property | Value |
|----------|-------|
| **Port** | 8003 |
| **Databases** | PostgreSQL (pet_service_db) + MongoDB (health_records_db) |
| **Key Tables** | pets, pet_types, health_records (metadata), vaccinations |
| **Events Produced** | pet.created, pet.updated, vaccination_due, health_record_added |
| **Events Consumed** | user.registered (owner), appointment.completed (visit record) |
| **Cache** | Redis (pet profiles, vaccination status) |

**API Endpoints**:
```
POST   /api/v1/pets                   # Owner/vet creates pet
GET    /api/v1/pets/{petId}           # Get pet profile
PUT    /api/v1/pets/{petId}           # Update pet info
GET    /api/v1/pets/{petId}/health-history  # Get health records
POST   /api/v1/pets/{petId}/health-records  # Add medical record
GET    /api/v1/pets/{petId}/vaccinations    # Vaccination status
POST   /api/v1/pets/{petId}/vaccinations    # Record vaccination
GET    /api/v1/owners/{ownerId}/pets  # List owner's pets
```

**Key Entities**:
```java
Pet (PostgreSQL)
  - id, ownerId, clinicId, name, petTypeId, birthDate
  - microchipId, breed, color, weight

Vaccination (PostgreSQL)
  - id, petId, name, date, nextDueDate, provider, clinic

HealthRecord (MongoDB - flexible schema)
  - id, petId, date, type, content
  - Example: { petId: "pet-123", date: "2026-04-15", type: "surgery", 
              surgeonName: "Dr. Smith", procedure: "spay", complications: null }

VaccinationSchedule (PostgreSQL)
  - petTypeId, vaccinationName, scheduleAgeInMonths, validityMonths, notes
```

**Technology**: Spring Boot, PostgreSQL, MongoDB, S3 (for documents), Redis

---

### 4. Appointment Service

**Responsibility**: Scheduling, availability, confirmations, status tracking

| Property | Value |
|----------|-------|
| **Port** | 8004 |
| **Database** | PostgreSQL (appointment_db) |
| **Key Tables** | appointments, availability_slots, confirmations, reminders |
| **Events Produced** | appointment.created, appointment.confirmed, appointment.cancelled, appointment.completed, reminder.scheduled |
| **Events Consumed** | pet.created, staff.assigned, clinic.hours.changed, user.registered |
| **Cache** | Redis (availability slots, vet schedules) |

**API Endpoints**:
```
POST   /api/v1/appointments           # Create appointment request
GET    /api/v1/appointments/{apptId}  # Get appointment details
PUT    /api/v1/appointments/{apptId}  # Update appointment
DELETE /api/v1/appointments/{apptId}  # Cancel appointment
POST   /api/v1/appointments/{apptId}/confirm   # Confirm by vet
GET    /api/v1/clinics/{clinicId}/availability # Get available slots
GET    /api/v1/vets/{vetId}/schedule  # Get vet's schedule
POST   /api/v1/appointments/{apptId}/complete  # Mark as completed
```

**Key Entities**:
```java
Appointment
  - id, petId, vetId, clinicId, ownerId
  - requestedDate, requestedTime, status (REQUESTED, CONFIRMED, COMPLETED, CANCELLED)
  - reason, notes, createdAt, confirmedAt, completedAt

AvailabilitySlot
  - id, vetId, clinicId, dateTime, durationMins
  - isAvailable, appointmentId (once booked)
  - createdAt, modifiedAt

AppointmentReminder
  - appointmentId, reminderType (BEFORE_1DAY, BEFORE_1HOUR)
  - scheduledFor, sentAt, channel (SMS, EMAIL, PUSH)
```

**Technology**: Spring Boot, PostgreSQL, Redis, Event Publisher

---

### 5. Notification Service

**Responsibility**: Send notifications (email, SMS, push), manage templates

| Property | Value |
|----------|-------|
| **Port** | 8005 |
| **Database** | PostgreSQL (notification_db) + Elasticsearch (audit log) |
| **Key Tables** | notification_templates, sent_notifications, delivery_attempts |
| **Events Consumed** | appointment.created, appointment.confirmed, appointment.cancelled, vaccination_due, user.registered, reminder.scheduled |
| **Integrations** | SendGrid (email), Twilio (SMS), Firebase (push) |

**API Endpoints**:
```
GET    /api/v1/notifications/{userId}      # Get notification history (100 latest)
POST   /api/v1/notifications/templates      # Admin manages templates
GET    /api/v1/notifications/delivery-status/{eventId}  # Check delivery status
```

**Key Entities**:
```java
NotificationTemplate
  - id, name, type (APPOINTMENT_CONFIRMED, VACCINATION_DUE, REMINDER)
  - channel (EMAIL, SMS, PUSH), subject, body (with placeholders)
  - createdBy, createdAt

SentNotification
  - id, templateId, recipientId, channel
  - content, status (SENT, DELIVERED, FAILED, BOUNCED)
  - sentAt, retries, lastError

Event consumed example:
{
  "eventId": "evt-123",
  "type": "appointment.created",
  "appointmentId": "appt-456",
  "petId": "pet-789",
  "ownerId": "owner-abc",
  "vetName": "Dr. Smith",
  "clinicName": "Main Clinic",
  "appointmentTime": "2026-05-01T14:00:00Z",
  "createdAt": "2026-04-21T10:30:00Z"
}
```

**Technology**: Spring Boot, PostgreSQL, Kafka Consumer, SendGrid, Twilio, Firebase Cloud Messaging

---

### Service Dependency Graph

```
┌─────────────────┐
│  API Gateway    │
└────────┬────────┘
         │
    ┌────┴──────────────────────┬────────┬──────────┐
    ↓                            ↓        ↓          ↓
┌─────────────┐         ┌──────────────┐ ┌──────┐ ┌──────────────┐
│User Service │◄────────│ Clinic Serv. │ │ Pet  │ │ Appt Service │
└─────────────┘         │              │ │Serv. │ └──────────────┘
    ▲                   └──────┬───────┘ └──┬───┘        ▲
    │                          │            │            │
    └──────────────────────────┼────────────┼────────────┘
                               │            │
                        ┌──────▼────────────▼──────┐
                        │   Message Broker (Kafka) │
                        │                          │
                        │ Topics:                  │
                        │ • user.* events         │
                        │ • clinic.* events       │
                        │ • pet.* events          │
                        │ • appointment.* events  │
                        │ • reminder.* events     │
                        └─────────┬────────────────┘
                                  │
                        ┌─────────▼──────────────┐
                        │ Notification Service   │
                        │ (Event Subscriber)     │
                        │ Sends email/SMS/push   │
                        └────────────────────────┘
```

**No Direct Service-to-Service Calls**: All inter-service communication is async via Kafka events and data replication (eventual consistency).

---

## Data Model Overview

### User Service Database Schema

```sql
-- Users and Authentication
CREATE TABLE users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  first_name VARCHAR(150) NOT NULL,
  last_name VARCHAR(150) NOT NULL,
  phone VARCHAR(20),
  profile_picture_url VARCHAR(500),
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  last_login TIMESTAMP
);

-- Roles
CREATE TABLE roles (
  id UUID PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL,  -- ADMIN, VET, CUSTOMER, STAFF
  description TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Role Permissions
CREATE TABLE permissions (
  id UUID PRIMARY KEY,
  resource VARCHAR(100),  -- e.g., "appointment", "pet"
  action VARCHAR(50),     -- e.g., "CREATE", "READ", "UPDATE", "DELETE"
  description TEXT
);

CREATE TABLE role_permissions (
  role_id UUID NOT NULL,
  permission_id UUID NOT NULL,
  FOREIGN KEY (role_id) REFERENCES roles(id),
  FOREIGN KEY (permission_id) REFERENCES permissions(id),
  PRIMARY KEY (role_id, permission_id)
);

-- User Roles (Multi-Clinic Support)
CREATE TABLE user_roles (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  role_id UUID NOT NULL,
  clinic_id UUID,  -- NULL for global roles (e.g., super-admin)
  granted_at TIMESTAMP DEFAULT NOW(),
  granted_by UUID,  -- admin user ID
  expires_at TIMESTAMP,
  is_active BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Audit Trail
CREATE TABLE audit_log (
  id UUID PRIMARY KEY,
  user_id UUID,
  action VARCHAR(255),
  resource_type VARCHAR(100),
  resource_id UUID,
  changes JSONB,  -- old values, new values
  timestamp TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### Clinic Service Database Schema

```sql
CREATE TABLE clinics (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  address VARCHAR(500),
  city VARCHAR(100),
  state VARCHAR(50),
  zip_code VARCHAR(10),
  phone VARCHAR(20),
  email VARCHAR(255),
  timezone VARCHAR(50),  -- e.g., "America/New_York"
  max_pets_per_day INT DEFAULT 10,
  max_appointment_duration_mins INT DEFAULT 30,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Clinic Operating Hours
CREATE TABLE operating_hours (
  id UUID PRIMARY KEY,
  clinic_id UUID NOT NULL,
  day_of_week INT,  -- 0 = Sunday, 1 = Monday, ..., 6 = Saturday
  open_time TIME,  -- e.g., "09:00:00"
  close_time TIME,  -- e.g., "17:00:00"
  is_open BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (clinic_id) REFERENCES clinics(id),
  UNIQUE (clinic_id, day_of_week)
);

-- Staff Assignments (Vets assigned to clinic)
CREATE TABLE staff_assignments (
  id UUID PRIMARY KEY,
  clinic_id UUID NOT NULL,
  user_id UUID NOT NULL,  -- Reference to User Service
  role_at_clinic VARCHAR(50),  -- "vet", "assistant", "receptionist"
  specialties VARCHAR(255),  -- JSON array: ["surgery", "dentistry"]
  start_date DATE,
  end_date DATE,
  is_primary BOOLEAN DEFAULT TRUE,  -- Primary clinic for this vet
  created_at TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (clinic_id) REFERENCES clinics(id)
);

-- Clinic Capacity Tracking
CREATE TABLE clinic_capacity (
  id UUID PRIMARY KEY,
  clinic_id UUID NOT NULL,
  date DATE,
  normal_capacity INT,
  emergency_capacity INT,
  booked_slots INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW(),
  UNIQUE (clinic_id, date),
  FOREIGN KEY (clinic_id) REFERENCES clinics(id)
);
```

### Pet Service Database Schema

```sql
-- Pet Types (Cached frequently)
CREATE TABLE pet_types (
  id UUID PRIMARY KEY,
  name VARCHAR(100) UNIQUE NOT NULL,  -- "Dog", "Cat", "Rabbit"
  description TEXT,
  average_lifespan_years INT
);

-- Pets
CREATE TABLE pets (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL,  -- Reference to User Service
  clinic_id UUID NOT NULL,  -- Primary clinic
  name VARCHAR(150) NOT NULL,
  pet_type_id UUID NOT NULL,
  birth_date DATE,
  microchip_id VARCHAR(50) UNIQUE,
  breed VARCHAR(150),
  color VARCHAR(150),
  weight_lbs DECIMAL(5, 2),
  gender VARCHAR(10),  -- "M", "F", "U"
  is_active BOOLEAN DEFAULT TRUE,
  notes TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (pet_type_id) REFERENCES pet_types(id)
);

-- Vaccination Records
CREATE TABLE vaccinations (
  id UUID PRIMARY KEY,
  pet_id UUID NOT NULL,
  clinic_id UUID NOT NULL,
  vaccine_name VARCHAR(150),  -- e.g., "Rabies", "FVRCP"
  administration_date DATE NOT NULL,
  expiration_date DATE,
  veterinarian_id UUID,  -- Reference to User Service
  vaccine_lot_number VARCHAR(50),
  notes TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (pet_id) REFERENCES pets(id)
);

-- Vaccination Schedule (Configurable by clinic)
CREATE TABLE vaccination_schedules (
  id UUID PRIMARY KEY,
  pet_type_id UUID NOT NULL,
  vaccine_name VARCHAR(150),
  age_in_months INT,  -- When to give this vaccine
  validity_months INT,  -- How long vaccine is valid
  notes TEXT,
  FOREIGN KEY (pet_type_id) REFERENCES pet_types(id)
);

-- Health Records Index (Metadata, actual data in MongoDB)
CREATE TABLE health_records (
  id UUID PRIMARY KEY,
  pet_id UUID NOT NULL,
  record_date DATE NOT NULL,
  record_type VARCHAR(50),  -- "visit", "surgery", "lab", "imaging"
  summary VARCHAR(500),
  veterinarian_id UUID,
  clinic_id UUID NOT NULL,
  mongodb_document_id VARCHAR(100),  -- Reference to MongoDB
  created_at TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (pet_id) REFERENCES pets(id)
);
```

### MongoDB Health Records Collection

```json
{
  "_id": "ObjectId(...)",
  "petId": "pet-789",
  "clinicId": "clinic-123",
  "veterinarianId": "user-456",
  "date": "2026-04-15",
  "type": "surgery",
  "title": "Spay Operation",
  "description": "Routine ovariohysterectomy performed successfully",
  "vital_signs": {
    "temperature_c": 37.8,
    "heart_rate_bpm": 85,
    "respiratory_rate": 20,
    "blood_pressure_systolic": 145
  },
  "surgical_details": {
    "procedure": "Ovariohysterectomy",
    "duration_minutes": 45,
    "anesthesia_type": "Isoflurane gas",
    "complications": "None",
    "stitches": 12,
    "recheck_in_days": 10
  },
  "prescriptions": [
    {
      "medication": "Carprofen 100mg",
      "dosage": "2 tablets",
      "frequency": "BID x 7 days",
      "reason": "Post-operative pain management"
    }
  ],
  "attachments": [
    {
      "s3_url": "s3://pet-clinic/health-records/pet-789/xray-2026-04-15.pdf",
      "file_type": "PDF",
      "description": "Pre-operative X-ray"
    }
  ],
  "next_steps": [
    "Monitor incision for discharge",
    "Recheck 10 days post-op",
    "Remove stitches after 14 days"
  ],
  "createdAt": "2026-04-15T14:30:00Z",
  "updatedAt": "2026-04-15T14:30:00Z"
}
```

### Appointment Service Database Schema

```sql
CREATE TABLE appointments (
  id UUID PRIMARY KEY,
  pet_id UUID NOT NULL,
  vet_id UUID NOT NULL,  -- Reference to User Service
  clinic_id UUID NOT NULL,
  owner_id UUID NOT NULL,  -- Reference to User Service
  requested_date_time TIMESTAMP NOT NULL,
  duration_minutes INT DEFAULT 30,
  status VARCHAR(50) NOT NULL,  -- REQUESTED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
  reason_for_visit TEXT,
  internal_notes TEXT,
  confirmed_at TIMESTAMP,
  completed_at TIMESTAMP,
  cancelled_at TIMESTAMP,
  cancellation_reason TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Availability Slots (Pre-computed by each vet/clinic)
CREATE TABLE availability_slots (
  id UUID PRIMARY KEY,
  vet_id UUID NOT NULL,
  clinic_id UUID NOT NULL,
  slot_date_time TIMESTAMP NOT NULL,
  duration_minutes INT,
  is_available BOOLEAN DEFAULT TRUE,
  appointment_id UUID,  -- FK to appointments if booked
  created_at TIMESTAMP DEFAULT NOW(),
  UNIQUE (vet_id, slot_date_time)
);

-- Appointment Reminders
CREATE TABLE appointment_reminders (
  id UUID PRIMARY KEY,
  appointment_id UUID NOT NULL,
  reminder_type VARCHAR(50),  -- BEFORE_1DAY, BEFORE_1HOUR
  scheduled_for TIMESTAMP,
  channel VARCHAR(50),  -- EMAIL, SMS, PUSH
  sent_at TIMESTAMP,
  status VARCHAR(50),  -- SCHEDULED, SENT, FAILED
  retry_count INT DEFAULT 0,
  last_error TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);
```

### Notification Service Database Schema

```sql
CREATE TABLE notification_templates (
  id UUID PRIMARY KEY,
  name VARCHAR(150) UNIQUE NOT NULL,
  trigger_event VARCHAR(100),  -- e.g., "appointment.created"
  channel VARCHAR(50),  -- EMAIL, SMS, PUSH
  subject VARCHAR(255),  -- For email
  body TEXT,  -- Template with {{placeholders}}
  placeholders JSONB,  -- { "appointmentTime": "ISO datetime", "vetName": "string" }
  is_active BOOLEAN DEFAULT TRUE,
  created_by UUID,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Example template:
-- {
--   "name": "appointment_reminder_1day",
--   "trigger_event": "reminder.scheduled",
--   "channel": "SMS",
--   "body": "Reminder: {{petName}} has an appointment with {{vetName}} at {{appointmentTime}} at {{clinicName}}. Confirm: {{confirmLink}}",
--   "placeholders": {
--     "petName": "string",
--     "vetName": "string",
--     "appointmentTime": "datetime",
--     "clinicName": "string",
--     "confirmLink": "url"
--   }
-- }

CREATE TABLE sent_notifications (
  id UUID PRIMARY KEY,
  template_id UUID NOT NULL,
  recipient_id UUID NOT NULL,  -- Reference to User Service
  channel VARCHAR(50),
  recipient_address VARCHAR(255),  -- email or phone number
  content TEXT,  -- Rendered template
  status VARCHAR(50),  -- SENT, DELIVERED, FAILED, BOUNCED, BLOCKED
  sent_at TIMESTAMP,
  delivered_at TIMESTAMP,
  bounced_at TIMESTAMP,
  retry_count INT DEFAULT 0,
  last_error TEXT,
  external_message_id VARCHAR(255),  -- From SendGrid/Twilio for tracking
  created_at TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (template_id) REFERENCES notification_templates(id)
);

-- Elasticsearch index for audit/search (ELK Stack)
PUT /notification-audit/_mappings
{
  "properties": {
    "notification_id": { "type": "keyword" },
    "recipient_id": { "type": "keyword" },
    "template_id": { "type": "keyword" },
    "channel": { "type": "keyword" },
    "status": { "type": "keyword" },
    "sent_at": { "type": "date" },
    "event_id": { "type": "keyword" },
    "content_preview": { "type": "text" },
    "clinic_id": { "type": "keyword" }
  }
}
```

### Event Schemas (Kafka Topics)

```json
-- Topic: user.registered
{
  "eventId": "evt-001",
  "eventType": "user.registered",
  "timestamp": "2026-04-21T10:00:00Z",
  "userId": "user-abc",
  "email": "john@example.com",
  "role": "customer",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1-555-1234"
}

-- Topic: clinic.created
{
  "eventId": "evt-002",
  "eventType": "clinic.created",
  "timestamp": "2026-04-21T10:15:00Z",
  "clinicId": "clinic-123",
  "name": "Downtown Vet Clinic",
  "city": "Portland",
  "timezone": "America/Los_Angeles"
}

-- Topic: appointment.created
{
  "eventId": "evt-003",
  "eventType": "appointment.created",
  "timestamp": "2026-04-21T10:30:00Z",
  "appointmentId": "appt-456",
  "petId": "pet-789",
  "ownerId": "owner-abc",
  "vetId": "vet-def",
  "clinicId": "clinic-123",
  "appointmentTime": "2026-05-01T14:00:00Z",
  "reasonForVisit": "Annual wellness check"
}

-- Topic: appointment.confirmed
{
  "eventId": "evt-004",
  "eventType": "appointment.confirmed",
  "timestamp": "2026-04-21T10:45:00Z",
  "appointmentId": "appt-456",
  "clinicId": "clinic-123",
  "ownerId": "owner-abc",
  "appointmentTime": "2026-05-01T14:00:00Z",
  "vetName": "Dr. Smith",
  "clinicName": "Downtown Vet Clinic",
  "petName": "Rex",
  "ownerEmail": "john@example.com",
  "ownerPhone": "+1-555-1234"
}

-- Topic: vaccination_due
{
  "eventId": "evt-005",
  "eventType": "vaccination_due",
  "timestamp": "2026-04-21T11:00:00Z",
  "petId": "pet-789",
  "ownerId": "owner-abc",
  "clinicId": "clinic-123",
  "petName": "Rex",
  "vaccineName": "Rabies",
  "daysOverdue": 0,
  "lastVaccinationDate": "2023-04-21",
  "ownerEmail": "john@example.com",
  "ownerPhone": "+1-555-1234"
}

-- Topic: appointment.completed
{
  "eventId": "evt-006",
  "eventType": "appointment.completed",
  "timestamp": "2026-04-21T14:45:00Z",
  "appointmentId": "appt-456",
  "clinicId": "clinic-123",
  "petId": "pet-789",
  "ownerId": "owner-abc",
  "vetId": "vet-def",
  "vetName": "Dr. Smith",
  "notes": "Pet is healthy. Annual vaccinations up to date.",
  "followUpNeeded": false
}
```

---

## API & Communication Design

### REST API Standards

#### Base URL Structure
```
https://api.petsandvets.com/api/v1
```

#### Standard Response Format

**Success Response (200, 201)**:
```json
{
  "success": true,
  "data": {
    "id": "user-abc-123",
    "email": "john@example.com",
    "firstName": "John",
    "role": "customer",
    "clinicsAssigned": ["clinic-123", "clinic-456"]
  },
  "timestamp": "2026-04-21T10:30:00Z",
  "requestId": "req-xyz-789"
}
```

**Error Response (4xx, 5xx)**:
```json
{
  "success": false,
  "error": {
    "code": "APPOINTMENT_SLOT_UNAVAILABLE",
    "message": "The requested time slot is no longer available",
    "details": "Slot was booked by another user at 2026-04-21T10:25:00Z",
    "timestamp": "2026-04-21T10:30:00Z",
    "path": "POST /api/v1/appointments",
    "requestId": "req-xyz-789"
  }
}
```

#### HTTP Status Codes

| Code | Meaning | Use Case |
|------|---------|----------|
| **200** | OK | GET successful, no data change |
| **201** | Created | POST successful, resource created |
| **204** | No Content | DELETE successful |
| **400** | Bad Request | Validation error, missing required fields |
| **401** | Unauthorized | No JWT or invalid token |
| **403** | Forbidden | Valid auth but insufficient permissions (e.g., vet tries to access different clinic) |
| **404** | Not Found | Resource doesn't exist |
| **409** | Conflict | Business logic violation (e.g., appointment slot taken) |
| **422** | Unprocessable Entity | Semantically wrong (e.g., pet age negative) |
| **429** | Too Many Requests | Rate limit exceeded |
| **500** | Server Error | Internal service error |
| **503** | Service Unavailable | Dependency down (e.g., database) |

#### Authentication & Headers

**All requests must include**:
```
Authorization: Bearer <JWT_TOKEN>
X-Request-ID: <UUID>  -- For tracing
X-Clinic-ID: <clinic-uuid>  -- Multi-clinic routing (optional but recommended for vets)
Content-Type: application/json
```

**JWT Claim Structure**:
```json
{
  "sub": "user-abc",
  "email": "john@example.com",
  "role": "vet",
  "clinicIds": ["clinic-123", "clinic-456"],
  "firstName": "John",
  "lastName": "Doe",
  "exp": 1714000000,
  "iat": 1713996400
}
```

### Core API Endpoints

#### User Service

```
POST /api/v1/auth/register
  Request:  { email, password, firstName, lastName, phone?, role }
  Response: JWT token, user object
  
POST /api/v1/auth/login
  Request:  { email, password }
  Response: JWT token, user object
  
POST /api/v1/auth/refresh
  Request:  { refreshToken }
  Response: New JWT token
  
GET /api/v1/users/{userId}
  Permissions: Self or admin
  Response:  { id, email, firstName, lastName, createdAt, roles }
  
GET /api/v1/users/{userId}/clinics
  Permissions: Self or admin
  Response:  List of clinics assigned to user [{ id, name, role }]
  
POST /api/v1/users/{userId}/clinics/{clinicId}
  Permissions: Admin only
  Request:  { role: "vet" | "staff" }
  Response: Assignment success
```

#### Clinic Service

```
POST /api/v1/clinics
  Permissions: Admin only
  Request:  { name, address, city, state, zipCode, phone, timezone, maxAppointmentDurationMins }
  Response: Clinic object with ID
  
GET /api/v1/clinics
  Permissions: Any authenticated
  Response: List of clinics (filtered by user's clinic assignments)
  
GET /api/v1/clinics/{clinicId}
  Permissions: User assigned to clinic + admin
  Response: Clinic details, hours, capacity summary
  
PUT /api/v1/clinics/{clinicId}
  Permissions: Clinic admin
  Request:  { name?, phone?, maxAppointmentDurationMins? }
  Response: Updated clinic object
  
GET /api/v1/clinics/{clinicId}/vets
  Permissions: Any authenticated
  Response: [{ userId, name, specialties, primary: true/false }]
  
POST /api/v1/clinics/{clinicId}/staff/{userId}
  Permissions: Clinic admin
  Request:  { role: "vet|staff", specialties?: ["surgery", "dentistry"], isPrimary }
  Response: Staff assignment object
  
GET /api/v1/clinics/{clinicId}/operating-hours
  Permissions: Any authenticated
  Response: { monday: { open: "09:00", close: "17:00" }, ... }
  
POST /api/v1/clinics/{clinicId}/operating-hours
  Permissions: Clinic admin
  Request:  { dayOfWeek, open_time, close_time }
  Response: Updated hours
```

#### Pet Service

```
POST /api/v1/pets
  Permissions: Owner (self) or vet/clinic staff
  Request:  { ownerId, clinicId, name, petTypeId, birthDate, breed?, color?, weight? }
  Response: Pet object with ID
  
GET /api/v1/pets/{petId}
  Permissions: Owner or vet from assigned clinic
  Response: Pet profile, current age, weight, last vaccination date
  
PUT /api/v1/pets/{petId}
  Permissions: Owner or vet from assigned clinic
  Request:  { name?, breed?, weight?, color? }
  Response: Updated pet object
  
GET /api/v1/owners/{ownerId}/pets
  Permissions: Owner (self) or clinic staff
  Query:    Page, size, filter by type
  Response: Paginated list of pets
  
GET /api/v1/pets/{petId}/health-history
  Permissions: Owner or vet from assigned clinic
  Query:    From_date?, to_date?, type? (visit|surgery|lab|imaging)
  Response: [{ date, type, summary, veterinarian, notes }]
  
POST /api/v1/pets/{petId}/health-records
  Permissions: Vet from assigned clinic
  Request:  { type, title, description, vital_signs?, prescriptions?, attachments? }
  Response: Health record with MongoDB reference
  
GET /api/v1/pets/{petId}/vaccinations
  Permissions: Owner or vet from assigned clinic
  Response: [{ vaccine_name, date, expiration_date, nextDueDate }]
  
POST /api/v1/pets/{petId}/vaccinations
  Permissions: Vet from assigned clinic
  Request:  { vaccine_name, administration_date, expiration_date, notes? }
  Response: Vaccination record
```

#### Appointment Service

```
POST /api/v1/appointments
  Permissions: Owner or receptionist
  Request:  { petId, clinicId, requestedDateTime, reasonForVisit, preferredVet?: vetId }
  Response: Appointment in REQUESTED status
  
GET /api/v1/appointments/{appointmentId}
  Permissions: Owner, assigned vet, clinic staff
  Response: Appointment details, vet name, status, confirmation time
  
PUT /api/v1/appointments/{appointmentId}
  Permissions: Owner (cancel only), vet/clinic (reschedule/confirm)
  Request:  { status: "confirmed"|"cancelled", newDateTime?, notes? }
  Response: Updated appointment
  
DELETE /api/v1/appointments/{appointmentId}
  Permissions: Owner or clinic admin (hard delete if not completed)
  Response: 204 No Content
  
GET /api/v1/clinics/{clinicId}/availability
  Permissions: Owner/receptionist for that clinic
  Query:    FromDate, toDate, petType?, preferredVet?
  Response: Available slots (date, time, duration, vet_name)
  ```
GET /api/v1/vets/{vetId}/schedule
  Permissions: Clinic admin or self
  Query:    FromDate, toDate
  Response: Vet's booked appointments, available slots
  
GET /api/v1/appointments?filter=owner_id,clinic_id,status,date_range
  Permissions: Owner (self), vet (clinic assignments), admin (all)
  Query:    Page, size, sortBy (date_asc, status)
  Response: Paginated appointment list
  
POST /api/v1/appointments/{appointmentId}/confirm
  Permissions: Assigned vet only
  Request:  { confirmedBy: vetId, notes? }
  Response: Appointment updated to CONFIRMED status, event published
  
POST /api/v1/appointments/{appointmentId}/complete
  Permissions: Assigned vet only
  Request:  { completedNotes, nextSteps?, prescribedMedicines? }
  Response: Appointment updated to COMPLETED, event published, triggers health record creation
```

### Event-Driven Communication

#### Kafka Topics & Schemas

| Topic | Producer | Consumer | Frequency |
|-------|----------|----------|-----------|
| user.registered | User Svc | Clinic Svc, Notification Svc | On signup |
| user.role_updated | User Svc | Clinic Svc, Appointment Svc | On role assignment |
| clinic.created | Clinic Svc | User Svc, Appointment Svc | Admin creates clinic |
| clinic.hours_updated | Clinic Svc | Appointment Svc | On hours change |
| staff.assigned | Clinic Svc | Appointment Svc | Admin assigns vet |
| pet.created | Pet Svc | Appointment Svc | Owner creates pet |
| pet.vaccination_recorded | Pet Svc | Notification Svc | Vaccination added |
| appointment.created | Appointment Svc | Pet Svc, Notification Svc | Customer books |
| appointment.confirmed | Appointment Svc | Notification Svc, Pet Svc | Vet confirms |
| appointment.cancelled | Appointment Svc | Notification Svc, Pet Svc | Cancel event |
| appointment.completed | Appointment Svc | Pet Svc, Notification Svc | Visit finalized |
| reminder.scheduled | Appointment Svc | Notification Svc | Before appointment |
| vaccination_due | Pet Svc (batch) | Notification Svc | Daily cron job |

#### Event Publishing Example (Spring Kafka)

```java
// In Appointment Service Controller
@PostMapping("/appointments/{appointmentId}/confirm")
public ResponseEntity<?> confirmAppointment(
    @PathVariable String appointmentId,
    @RequestBody ConfirmRequest request,
    @RequestHeader String authorization) {
    
    Appointment appt = appointmentService.confirmAppointment(
        appointmentId, 
        request.getVetId()
    );
    
    // Publish event
    AppointmentConfirmedEvent event = new AppointmentConfirmedEvent(
        eventId = UUID.randomUUID(),
        appointmentId = appointmentId,
        petId = appt.getPetId(),
        ownerId = appt.getOwnerId(),
        clinicId = appt.getClinicId(),
        appointmentTime = appt.getRequestedDateTime(),
        vetName = appt.getVet().getFullName(),
        timestamp = Instant.now()
    );
    
    kafkaTemplate.send(APPOINTMENT_CONFIRMED_TOPIC, eventId, event);
    
    return ResponseEntity.ok(appt);
}
```

#### Event Consuming Example (Notification Service)

```java
@KafkaListener(topics = "appointment.confirmed", groupId = "notification-service")
public void handleAppointmentConfirmed(
    AppointmentConfirmedEvent event,
    @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp) {
    
    // Fetch template
    NotificationTemplate template = templateRepo.findByName(
        "appointment_confirmed_customer"
    );
    
    // Render with event data
    String body = template.getBody()
        .replace("{{vetName}}", event.getVetName())
        .replace("{{appointmentTime}}", event.getAppointmentTime())
        .replace("{{petName}}", event.getPetName());
    
    // Send via all channels configured
    sendEmail(event.getOwnerEmail(), template.getSubject(), body);
    sendSMS(event.getOwnerPhone(), body);
    sendPushNotification(event.getOwnerId(), body);
    
    // Record in sent_notifications table
    notificationRepo.save(new SentNotification(
        templateId = template.getId(),
        recipientId = event.getOwnerId(),
        status = "SENT",
        sentAt = Instant.now()
    ));
}
```

### API Gateway Configuration (Kong / AWS API Gateway)

**Routing Rules**:
```
/api/v1/auth/** → User Service (port 8001)
/api/v1/clinics/** → Clinic Service (port 8002), USER_SERVICE (port 8001)
/api/v1/pets/** → Pet Service (port 8003), REQUIRES jwt
/api/v1/appointments/** → Appointment Service (port 8004), REQUIRES jwt
/health → Service health checks
/metrics → Prometheus metrics
```

**Rate Limiting**:
```
- Anonymous (no JWT): 100 requests/hour
- Authenticated: 10,000 requests/hour
- Premium (future): Unlimited
```

**Authentication Plugin** (API Gateway):
```
- Extract JWT from Authorization header
- Validate signature (using User Service's public key)
- Pass claims to downstream service via X-User-Claims header
- Reject if expired or invalid
```

---

## Cloud & Deployment Strategy

### AWS Architecture (Reference Cloud)

```
┌─────────────────────────────────────────────────────────────────┐
│                         AWS REGION                              │
│ (us-east-1, with multi-AZ for high availability)               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Internet Gateway                                               │
│        ↓                                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            Application Load Balancer (ALB)               │  │
│  │        (Public Subnet, Multi-AZ)                         │  │
│  └────────────────┬─────────────────────────────────────────┘  │
│                   │                                              │
│  ┌────────────────▼─────────────────────────────────────────┐  │
│  │            AWS API Gateway (Optional layer)              │  │
│  │  - JWT Validation                                        │  │
│  │  - Rate Limiting (100K requests/min)                     │  │
│  │  - Logging, WAF integration                              │  │
│  └────────────────┬─────────────────────────────────────────┘  │
│                   │                                              │
│  ┌────────────────┴─────────────────────────────────────────┐  │
│  │    KUBERNETES CLUSTER (Managed: EKS)                     │  │
│  │  ┌──────────────────────────────────────────────────┐   │  │
│  │  │  Node Group 1 (us-east-1a)                      │   │  │
│  │  │  ├─ user-service Pod (replica-set: 2)          │   │  │
│  │  │  ├─ clinic-service Pod (replica-set: 2)        │   │  │
│  │  │  └─ pet-service Pod (replica-set: 2)           │   │  │
│  │  └──────────────────────────────────────────────────┘   │  │
│  │                                                          │  │
│  │  ┌──────────────────────────────────────────────────┐   │  │
│  │  │  Node Group 2 (us-east-1b)                      │   │  │
│  │  │  ├─ appointment-service Pod (replica-set: 3)    │   │  │
│  │  │  └─ notification-service Pod (replica-set: 2)   │   │  │
│  │  └──────────────────────────────────────────────────┘   │  │
│  │                                                          │  │
│  │  ┌──────────────────────────────────────────────────┐   │  │
│  │  │  Autoscaler (HPA)                               │   │  │
│  │  │  - CPU > 70% → +1 pod                           │   │  │
│  │  │  - Memory > 80% → +1 pod                        │   │  │
│  │  │  - Max 10 pods per service                      │   │  │
│  │  └──────────────────────────────────────────────────┘   │  │
│  │                                                          │  │
│  │  ┌──────────────────────────────────────────────────┐   │  │
│  │  │  Ingress Controller (NGINX / AWS LB)            │   │  │
│  │  │  - Route /api/v1/users/* → user-service         │   │  │
│  │  │  - Route /api/v1/clinics/* → clinic-service     │   │  │
│  │  │  - TLS/HTTPS termination                        │   │  │
│  │  └──────────────────────────────────────────────────┘   │  │
│  │                                                          │  │
│  │  ┌──────────────────────────────────────────────────┐   │  │
│  │  │ Service Mesh (Istio - Optional) Layer 5-7       │   │  │
│  │  │ - Service-to-service mutual TLS                 │   │  │
│  │  │ - Circuit breaking, retry policies              │   │  │
│  │  │ - Distributed tracing (Jaeger sidecar)          │   │  │
│  │  └──────────────────────────────────────────────────┘   │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
├─ MANAGED DATABASES (Private Subnets, Multi-AZ) ──────────────┤
│                                                                 │
│  ┌──────────────────────┐  ┌──────────────────────┐           │
│  │  RDS PostgreSQL      │  │  AWS ElastiCache     │           │
│  │  (Multi-AZ, 2 read   │  │  (Redis Cluster)     │           │
│  │   replicas)          │  │                      │           │
│  │                      │  │  - Session cache     │           │
│  │  ├─ user_db          │  │  - Vet availability  │           │
│  │  ├─ clinic_db        │  │  - Rate limiting     │           │
│  │  ├─ pet_db           │  │  - Appointment slots │           │
│  │  └─ appointment_db   │  │                      │           │
│  └──────────────────────┘  └──────────────────────┘           │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  AWS DocumentDB (MongoDB Compatible, Multi-AZ)          │  │
│  │  - Pet health records (flexible schema)                 │  │
│  │  - Automatic backups                                    │  │
│  │  - Encryption at rest & in transit                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
├─ MESSAGE BROKER ──────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Amazon MSK (Managed Kafka)                             │  │
│  │  - 3 brokers  (Multi-AZ)                                │  │
│  │  - Auto-scaling partitions based on throughput           │  │
│  │  - Encryption, IAM auth, IP whitelist                   │  │
│  │  - CloudWatch monitoring                                │  │
│  │                                                          │  │
│  │  Topics:                                                │  │
│  │  ├─ user.* (replication: 2)                            │  │
│  │  ├─ clinic.* (replication: 2)                          │  │
│  │  ├─ pet.* (replication: 2)                             │  │
│  │  └─ appointment.* (replication: 3) [High importance]   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
├─ OBJECT STORAGE & CDN ─────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────┐     ┌─────────────────────────────┐ │
│  │  Amazon S3           │     │  CloudFront CDN             │ │
│  │                      │     │                             │ │
│  │  ├─ pet-clinic/     ├──→  │  - Caches all static assets │ │
│  │  │  ├─ images/      │     │  - DDoS protection (Shield) │ │
│  │  │  ├─ records/     │     │  - 200+ edge locations      │ │
│  │  │  └─ documents/   │     │                             │ │
│  │  │                  │     │  - TTL: 1 day (default)    │ │
│  │  └─ Encryption: AES256   │  - Access logs to S3        │ │
│  │  └─ Versioning: Enabled  └─────────────────────────────┘ │
│  │  └─ Lifecycle: Archive to Glacier after 90 days        │  │
│  └──────────────────────┘                                   │ │
│                                                                 │
├─ LOGGING & MONITORING ────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  CloudWatch Logs (Centralized Logging)                  │  │
│  │  - All pod logs streamed via Fluentd                    │  │
│  │  - Log retention: 30 days (configurable)                │  │
│  │  - Custom log groups per service                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Prometheus + Grafana (Metrics & Visualization)         │  │
│  │  - Pod resource usage (CPU, memory)                     │  │
│  │  - Application metrics (response time, throughput)      │  │
│  │  - Database query performance                          │  │
│  │  - Dashboards per service                              │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Jaeger (Distributed Tracing)                           │  │
│  │  - Trace requests across services                       │  │
│  │  - Correlation IDs in all requests                      │  │
│  │  - Latency analysis per service hop                     │  │
│  │  - Error tracking                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  CloudWatch Alarms                                       │  │
│  │  - Error rate > 5% → PagerDuty alert                    │  │
│  │  - Latency p99 > 500ms → Slack alert                    │  │
│  │  - DB CPU > 80% → Auto-scale RDS read replicas         │  │
│  │  - Pod CrashLoopBackOff → Immediate page               │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Deployment Pipeline

```
┌──────────────────────┐
│  Developer commits   │
│  code to feature/    │
│  branch             │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────────────────────────────────────┐
│  GitHub Actions / GitLab CI                          │
│  Pipeline Trigger (on PR, commit to main)            │
│                                                      │
│  Stage 1: BUILD                                      │
│  ├─ Compile Java code                               │
│  ├─ Run unit tests                                  │
│  ├─ SonarQube code quality scan                      │
│  └─ Build Docker image (multi-stage)                │
│     └─ Push to Amazon ECR                           │
│                                                      │
│  Stage 2: TEST                                       │
│  ├─ Integration tests (Testcontainers)              │
│  ├─ Contract tests (Pact)                           │
│  └─ Security scan (Trivy, Anchore)                  │
│                                                      │
│  Stage 3: DEPLOY TO STAGING                         │
│  ├─ Helm update staging EKS cluster                 │
│  ├─ Run smoke tests                                 │
│  └─ Notify team (Slack)                             │
│                                                      │
│  Stage 4: APPROVAL                                  │
│  └─ Manual approval for production                  │
│                                                      │
│  Stage 5: DEPLOY TO PRODUCTION (Blue-Green)         │
│  ├─ Deploy new version (Green)                      │
│  ├─ Health checks pass?                             │
│  │  ├─ YES → Route 100% traffic to Green            │
│  │  └─ NO → Rollback to Blue, alert ops             │
│  └─ Decommission old version (Blue)                 │
│                                                      │
│  Stage 6: POST-DEPLOYMENT                           │
│  ├─ Synthetic monitoring (smoke tests)              │
│  ├─ Verify metrics (error rate, latency)            │
│  ├─ Send deployment report                          │
│  └─ Archive release notes to wiki                   │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### Deployment Configuration (Helm Charts)

**Each service has its own Helm chart**:

```yaml
# values-prod.yaml for appointment-service
replicaCount: 3
image:
  repository: 123456789.dkr.ecr.us-east-1.amazonaws.com/appointment-service
  tag: "1.2.3"
  pullPolicy: IfNotPresent

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

service:
  type: ClusterIP
  port: 8004

env:
  - name: SPRING_PROFILES_ACTIVE
    value: "production"
  - name: KAFKA_BROKERS
    valueFrom:
      configMapKeyRef:
        name: kafka-config
        key: brokers
  - name: DATABASE_URL
    valueFrom:
      secretKeyRef:
        name: appointment-db-secret
        key: connection-url

livenessProbe:
  httpGet:
    path: /api/v1/health
    port: 8004
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /api/v1/health/readiness
    port: 8004
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 2
```

### Scaling Strategy

| Service | Min Replicas | Max Replicas | Scale Trigger |
|---------|--------------|--------------|---------------|
| **User Service** | 2 | 6 | CPU > 70% or Memory > 80% |
| **Clinic Service** | 2 | 4 | CPU > 70% |
| **Pet Service** | 2 | 5 | CPU > 70% |
| **Appointment Service** | 3 | 10 | CPU > 60% (high traffic expected) |
| **Notification Service** | 2 | 8 | Kafka lag > 10k messages |

### Disaster Recovery

**Backup Strategy**:
```
- RDS: Daily automated backups, 30-day retention, cross-region copy
- DocumentDB: Continuous backup enabled, point-in-time recovery (35 days)
- S3: Versioning enabled, cross-region replication for critical buckets
- Kafka: Replication factor 2-3, retention 7 days
```

**Recovery Time Objectives (RTO) & Recovery Point Objectives (RPO)**:
```
- Database corruption: RTO = 1 hour, RPO = 1 hour (restore from backup)
- Service pod failure: RTO = 2 minutes (auto-restart), RPO = 0 (stateless)
- Region failure: RTO = 30 minutes, RPO = 5 minutes (failover to DR region)
- Data center failure: RTO = < 1 second (multi-AZ), RPO = < 1 second
```

---

## Security & Multi-Tenancy

### Authentication & Authorization Architecture

```
┌──────────────────────────────────────┐
│ User Login Request                   │
│ POST /api/v1/auth/login              │
│ { email, password }                  │
└──────────────┬───────────────────────┘
               ▼
┌──────────────────────────────────────┐
│ User Service                         │
│ 1. Validate email exists             │
│ 2. Hash password, compare            │
│ 3. Generate JWT token with claims    │
│ 4. Refresh token stored in Redis     │
└──────────────┬───────────────────────┘
               ▼
┌──────────────────────────────────────┐
│ JWT Response                         │
│ {                                    │
│   "access_token": "eyJhbG...",       │
│   "refresh_token": "eyJyZW...",      │
│   "expires_in": 3600                 │
│ }                                    │
└──────────────┬───────────────────────┘
               ▼
┌──────────────────────────────────────┐
│ Subsequent API Requests              │
│ Header: Authorization: Bearer ...    │
└──────────────┬───────────────────────┘
               ▼
┌──────────────────────────────────────┐
│ API Gateway JWT Validation           │
│ 1. Extract token from header         │
│ 2. Verify signature (RS256)          │
│ 3. Check expiration                  │
│ 4. Pass claims to service            │
│ (If invalid, return 401)             │
└──────────────┬───────────────────────┘
               ▼
┌──────────────────────────────────────┐
│ Downstream Service Logic             │
│ 1. Extract clinicId from claims      │
│ 2. Query database with clinic filter │
│ 3. Check role permissions            │
│ 4. Return results or 403 if denied   │
└──────────────────────────────────────┘
```

### JWT Token Anatomy

```
Header:
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "2024-prod-key-1"
}

Payload:
{
  "sub": "user-uuid",
  "email": "john@example.com",
  "role": "vet",
  "clinicIds": ["clinic-123", "clinic-456"],
  "firstName": "John",
  "lastName": "Doe",
  "permissions": ["appointment:read", "pet:write", "clinic:read"],
  "iat": 1713996400,
  "exp": 1714000000,
  "aud": "api.petsandvets.com",
  "iss": "user-service.petsandvets.com",
  "jti": "jwt-jti-12345"
}

Signature:
RS256(base64Url(header) + '.' + base64Url(payload), private_key)
```

### Multi-Tenancy Data Isolation

**At Database Layer**:
```sql
-- Every table has clinic_id or user_id + clinic_id composite key
CREATE TABLE appointments (
  id UUID PRIMARY KEY,
  clinic_id UUID NOT NULL,  -- ALWAYS included
  pet_id UUID NOT NULL,
  vet_id UUID NOT NULL,
  owner_id UUID NOT NULL,
  ...
);

-- Indexes on clinic_id for fast filtering
CREATE INDEX idx_appointments_clinic_id ON appointments(clinic_id);

-- Row-level security (if using PostgreSQL RLS)
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;

CREATE POLICY clinic_isolation ON appointments
  FOR ALL
  USING (clinic_id = current_setting('app.clinic_id')::uuid);
```

**At API Layer**:
```java
@GetMapping("/api/v1/appointments")
public List<Appointment> getAppointments(
    @RequestHeader String authorization,
    @RequestParam(required = false) String clinicId) {
    
    Claims claims = jwtValidator.validateAndExtract(authorization);
    String userId = claims.getSubject();
    List<String> userClinicIds = (List<String>) claims.get("clinicIds");
    
    // Validate clinicId parameter (if provided) is in user's clinic list
    if (clinicId != null && !userClinicIds.contains(clinicId)) {
        throw new AccessDeniedException("Not assigned to clinic: " + clinicId);
    }
    
    // If no clinicId provided, use first clinic
    String effectiveClinicId = clinicId != null ? clinicId : userClinicIds.get(0);
    
    // Query ALWAYS includes clinic filter
    return appointmentRepo.findByClinicId(effectiveClinicId);
}
```

### Role-Based Access Control (RBAC)

**Roles & Permissions Matrix**:

| Role | Appointments | Pets | Customers | Vets | Clinic Mgmt | Notifications |
|------|--------------|------|-----------|------|-------------|---------------|
| **SUPER_ADMIN** | Full | Full | Full | Full | Full | Full |
| **CLINIC_ADMIN** | Read/Write | Read/Write | Read/Write | Assign | Full (own clinic) | Read |
| **VET** | Read/Confirm | Read/Write | Read (own pets) | View peers | Read (own clinic) | Read (own) |
| **STAFF** | Read | Read | Read | Read | None | None |
| **CUSTOMER** | Own only | Own only | Own profile | View | None | Own only |

### Encryption & Secrets

**At Rest**:
- Database encryption: AWS RDS encryption, AES-256
- S3 encryption: Server-side encryption, AES-256
- Secrets in AWS Secrets Manager (rotated monthly)

**In Transit**:
- TLS 1.3 for all external API calls
- Certificate validation on service-to-service calls (mTLS via Istio)
- API Gateway enforces HTTPS, redirects HTTP → HTTPS

**Secrets Management**:
```
Environment Variables (never hardcoded):
- DATABASE_PASSWORD ← AWS Secrets Manager
- JWT_PRIVATE_KEY ← AWS Secrets Manager (RS256)
- API_KEYS (SendGrid, Twilio) ← AWS Secrets Manager
- ENCRYPTION_KEY ← AWS Secrets Manager

Rotation:
- API keys: Monthly
- Database credentials: Quarterly
- JWT signing key: Annually or on compromise
```

### Data Privacy & Compliance

**GDPR/HIPAA Compliance**:
1. **Data Retention**: Deleted pet records archival after 7 years per HIPAA
2. **Right to Erasure**: Customer can request all data deletion (triggers async job)
3. **Audit Trail**: All changes logged with user ID, timestamp, old/new values
4. **Encryption**: PII (phone, address) encrypted at rest
5. **Access Logs**: 90-day retention, reviewed monthly for unusual access

**Example Audit Trail**:
```json
{
  "audit_id": "audit-123",
  "timestamp": "2026-04-21T10:30:00Z",
  "user_id": "vet-456",
  "action": "UPDATE_PET",
  "resource_id": "pet-789",
  "old_values": {
    "weight_lbs": 45.5,
    "last_vaccine": "2025-12-15"
  },
  "new_values": {
    "weight_lbs": 46.0,
    "last_vaccine": "2026-04-21"
  },
  "status": "SUCCESS",
  "clinic_id": "clinic-123"
}
```

---

## Migration Plan

### Phase Overview

```
Phase 1: Foundation (Months 1-2)
├─ Set up cloud infrastructure (AWS EKS, RDS, Kafka)
├─ Deploy API Gateway
├─ Create User Service (extract from monolith)
└─ Set up CI/CD pipeline

Phase 2: Core Services (Months 3-4)
├─ Extract Clinic Service & Pet Service
├─ Deploy event streaming (Kafka)
├─ Migrate user data

Phase 3: Operations (Months 5-6)
├─ Extract Appointment Service
├─ Deploy Notification Service
├─ Migrate appointment/visit data

Phase 4: Validation & Optimization (Months 7-9)
├─ Run parallel systems (monolith + microservices)
├─ Performance testing & tuning
├─ Security audit & penetration testing

Phase 5: Cutover (Months 10-12)
├─ Switch traffic to microservices
├─ Monitor for issues
├─ Decommission monolith
├─ Debrief & documentation
```

### Detailed Phase 1: Foundation Setup

**Week 1-2: Infrastructure**
```
1. Create AWS VPC, subnets, security groups
2. Deploy EKS cluster (Kubernetes 1.29+)
   - 2 node groups (on-demand + spot instances)
   - Auto-scaling configured (min 3, max 20 nodes)
   - Istio service mesh (optional, for phase 5)
3. Provision RDS PostgreSQL Multi-AZ (user_db, clinic_db, pet_db, appointment_db)
4. Provision DocumentDB for health records
5. Provision ElastiCache Redis cluster
6. Set up MSK (Kafka) with 3 brokers
7. Configure CloudWatch, Jaeger, Prometheus
```

**Week 3: API Gateway & User Service**
```
1. Deploy AWS API Gateway
   - Create custom authorizer (validates JWT)
   - Configure rate limiting (10K req/min per user)
   - Set up routing rules to services
2. Build User Service (Spring Boot)
   - JWT generation (RS256 signing)
   - User registration/login endpoints
   - Role management
   - Integration tests
3. Deploy to EKS (Helm chart)
4. Integration test from API Gateway
```

**Week 4: CI/CD & Monitoring**
```
1. Set up GitHub Actions pipeline
   - Build → Unit tests → Docker build → ECR push
   - Integration tests (Testcontainers)
   - Security scanning (Trivy)
2. Deploy staging environment
3. Set up CloudWatch alarms (error rate, latency)
4. Documentation: Architecture diagrams, deployment runbooks
```

### Detailed Phase 2: Core Services Migration

**Week 5-6: Pet Service Extraction**
```
1. Extract Pet entity logic from monolith:
   - Create pet_db PostgreSQL schema
   - Copy pet type data
   - Set up Pet Service (Spring Boot)
2. Implement Pet Service API:
   - GET/POST /api/v1/pets
   - GET /api/v1/pets/{id}/health-history
   - Pub events (pet.created, pet.updated)
3. Set up MongoDB for health records
4. Data migration: Bulk copy pets from monolith
5. Integration tests with Testcontainers
6. Deploy to EKS staging
7. Parallel test: Call both monolith and Pet Service, compare results
```

**Week 7-8: Clinic Service Extraction**
```
1. Create clinic_db PostgreSQL schema
2. Build Clinic Service
3. Implement endpoints:
   - GET/POST /api/v1/clinics
   - Staff assignment endpoints
   - Operating hours endpoints
4. Event publishing (clinic.created, staff.assigned)
5. Data migration: Seed initial clinic data (demo data for existing single clinic)
6. Deploy to EKS
7. Integration testing
```

### Detailed Phase 3: Appointment & Notifications

**Week 9-10: Appointment Service**
```
1. Create appointment_db PostgreSQL
2. Build Appointment Service
3. Endpoints:
   - POST /api/v1/appointments (booking)
   - GET /api/v1/appointments/{id}
   - PUT /api/v1/appointments/{id}/confirm
   - Availability query
4. Consume events (pet.created, staff.assigned)
5. Publish events (appointment.created, appointment.confirmed, appointment.completed)
6. Availability slot pre-computation (cron job)
7. Data migration: Copy visits as appointments
```

**Week 11-12: Notification Service**
```
1. Build Notification Service
2. Subscribe to events:
   - appointment.created → Send confirmation request notification
   - appointment.confirmed → Send confirmed notification to customer
   - vaccination_due → Send reminder (daily cron)
3. Template engine for email/SMS rendering
4. Integrations:
   - SendGrid for email
   - Twilio for SMS
   - Firebase for push notifications
5. Retry logic for failed sends (exponential backoff)
6. Deploy to EKS
```

### Detailed Phase 4: Validation & Optimization

**Week 13-16: Parallel Operations**
```
1. Both systems running simultaneously:
   - Monolith: Responds to existing traffic
   - Microservices: Shadow traffic + testing
2. Load testing:
   - Simulate 1K concurrent users
   - Verify latency < 200ms p99
   - Verify error rate < 0.1%
3. Chaos engineering (intentional failures):
   - Kill pods, verify auto-recovery
   - Database failover, verify no data loss
   - Kafka broker failure, verify event processing continues
4. Database optimization:
   - Add missing indexes
   - Tune connection pools
   - Query analysis & optimization
5. Security audit:
   - Penetration testing by 3rd party
   - JWT token validation testing
   - Multi-tenancy isolation verification
6. Documentation:
   - Operations runbooks
   - Troubleshooting guides
   - Architecture decision records (ADRs)
```

### Detailed Phase 5: Cutover & Deprecation

**Week 17-18: Traffic Shift**
```
1. Blue-green deployment:
   - Blue = Monolith (current production)
   - Green = Microservices (new)
2. Canary deployment:
   - Route 5% traffic to Green for 1 hour
   - Monitor error rates, latency
   - If good, route 25%, then 50%, then 100%
   - If bad, immediate rollback to Blue
3. Post-deployment validation:
   - Synthetic tests (booking appointment end-to-end)
   - Customer smoke testing (10 power users)
   - 24-hour monitoring (error rates, P99 latency)
4. Communicate with users/vets (email + in-app banner)
```

**Week 19+: Monolith Deprecation**
```
1. Monitor microservices for 1 week (zero production issues)
2. Archive monolith codebase
3. Decommission monolith infrastructure (after 30-day retention)
4. Celebrate! 🎉
5. Post-mortem: Lessons learned, optimizations discovered
6. Roadmap for next phase:
   - Mobile app push notifications
   - Advanced analytics (busiest days, common procedures)
   - Veterinarian marketplace (connect multiple clinics)
```

### Data Migration Strategy

**For Each Service:**

```
Step 1: Full migration (before Phase)
├─ Extract data from monolith
├─ Transform to target schema
├─ Load into new database
└─ Validate counts match

Step 2: Shadowing (during Phase)
├─ Monolith continues serving traffic
├─ Microservice runs in read-only parallel
├─ Compare responses for data consistency
└─ Fix any discrepancies

Step 3: Cutover (at end of Phase)
├─ Stop writes to monolith
├─ Final sync of new records
├─ Switch traffic to microservice
└─ Monitor for issues

Example (Pet Service):
Before Phase 2:
SELECT COUNT(*) FROM pets;  -- monolith: 50,000 pets
INSERT INTO pets (SELECT * FROM monolith.pets);  -- pet_db: 50,000 pets

During Phase 2:
-- Monolith still inserts new pets
-- Pet Service reads only, compares counts daily

After Phase 2:
-- Monolith stops inserting pets
-- Pet Service begins receiving writes
```

### Rollback Plan

**If critical issues arise during cutover:**

```
Cutover T+0 hours: 100% traffic to microservices
Cutover T+1 hour: Error rate 5% → Automatic rollback to monolith
1. API Gateway routes 100% back to monolith
2. Incident declared
3. Team pulls to debug microservices
4. Once fixed (2-4 hours), attempt Canary again
5. If 2 failures, defer Phase 5 by 2 weeks for remediation
```

---

## Trade-offs & Considerations

### Complexity vs. Flexibility

| Aspect | Monolith | Microservices |
|--------|----------|---------------|
| **Deployment** | 1 unit | 5 independent units |
| **Debugging** | Single process, single logs | Distributed traces, correlation IDs needed |
| **Testing** | Full integration simple | Contract tests, service mocks needed |
| **Team Structure** | Single team | Needs 5 small teams or strong coordination |
| **Learning Curve** | Easier for junior devs | More infrastructure/DevOps skills needed |
| **Cost (Infrastructure)** | Lower | Higher (5 databases, more services) |
| **Cost (Team Effort)** | Lower | Higher initially, lower later (faster shipping) |
| **Failure Isolation** | Monolithic failure | Pet Service down only affects pet operations |
| **Scaling** | Scale entire system | Scale only Appointment Service during rush |

**Recommendation**: The multi-clinic requirement + scaling needs justify microservices complexity. However, consider phasing: Years 1-2 modular monolith, years 2-3 gradual service extraction.

### Database Choices

| Choice | Pros | Cons |
|--------|------|------|
| **Shared schema** | ACID transactions, simple joins | No service independence, scaling locked together |
| **Database per service** | Maximum independence, scaling | Eventual consistency, no distributed transactions, data replication overhead |
| **API layer joins** | Keep independent DBs | N+1 query problems, latency, complexity |

**What We Chose**: Database per service (eventual consistency via events).

**Alternative**: Shared read replicas (Pet Service can read from Appointment DB for reference) + events for writes.

### Synchronous vs. Asynchronous

| Pattern | Latency Impact | Consistency | Complexity |
|---------|----------------|-------------|-----------|
| **Sync REST** | +100ms per hop (P2P Service calls) | Strong consistency | Lower (traditional request/response) |
| **Async Events** | +5-50ms (event processing) | Eventual consistency | Higher (requires saga patterns) |
| **Hybrid** | Sync for critical path (booking), async for non-blocking (notifications) | Mixed | Medium |

**What We Chose**: Hybrid. Booking appointment is sync (immediate response), notification is async.

**Trade-off**: Users don't see confirmation immediately if Notification Service is slow, but appointment is guaranteed booked.

---

## Final Summary & Recommendations

### Architecture Achievements

✅ **Multi-Clinic Support**: System designed from ground up for multi-tenancy  
✅ **Scalability**: Independent service scaling based on demand patterns  
✅ **Resilience**: Service failures isolated, circuit breakers prevent cascades  
✅ **Operational Visibility**: Distributed tracing, centralized logging, monitoring  
✅ **Maintainability**: Bounded contexts (DDD) → easier to reason about & modify  
✅ **Cloud-Ready**: 12-factor app design, container-native, stateless services  
✅ **Future-Proof**: Event-driven allows easy integration of new services (Analytics, Reporting, AI)  

### Critical Success Factors

1. **Team Alignment**: Microservices require cross-functional coordination (DevOps, DBAs, Developers)
2. **CI/CD Maturity**: Automated testing, deployment pipelines non-negotiable
3. **Monitoring & Alerting**: Without observability, troubleshooting becomes expensive nightmare
4. **Clear API Contracts**: Teams need agreement on data formats early (JSON Schema, OpenAPI)
5. **Dedicated Platform Team**: Manage infrastructure, Kubernetes, secrets, deployment automation
6. **Communication**: Service teams must understand event schemas, API changes

### Investments Required

**Year 1**:
- Infrastructure: $50-80K (AWS EKS, RDS, Kafka, monitoring)
- Tooling: $30K (CI/CD, APM, security scanning)
- Team upskilling: $20K (training, certifications)
- **Total Year 1**: ~$100-130K

**Year 2+**:
- Operations: $80-120K annually (cloud costs + team time)
- **ROI**: 40% faster feature delivery, 50% less unplanned downtime

### Next Steps (Recommended Roadmap)

1. **Month 1**: Review this document with stakeholders, align on phasing
2. **Month 2-3**: Build Phase 1 infrastructure (AWS account setup, EKS cluster, CI/CD)
3. **Month 4-5**: Extract & deploy User Service + Clinic Service
4. **Month 6-7**: Extract & deploy Pet Service + Appointment Service
5. **Month 8-9**: Build Notification Service, configure Kafka topics
6. **Month 10-12**: Parallel testing, load testing, cutover
7. **Year 2**: Monitoring optimization, AI-driven scheduling (future enhancement)
8. **Year 3**: Mobile app (leveraging headless architecture)

### Out-of-Scope (Future Enhancements)

- Session persistence (assumes stateless services)
- GraphQL API layer (could add as API gateway option)
- Multi-region replication (assume single-region for year 1)
- Machine learning pipeline (scheduling optimization)
- IoT integration (telemedicine, IP cameras)
- Blockchain audit trail (regulatory overkill for current needs)

### Conclusion

Modernizing the "Pets & Vets" application to a microservices architecture is a **12-18 month journey** with significant upfront investment but long-term operational gains. The system will be:

- **Scalable**: Handle growth from 1 clinic to 1000+ clinic network
- **Maintainable**: Service teams can develop/deploy independently
- **Resilient**: Failures isolated, graceful degradation possible
- **Observable**: All operations visible, SRE-friendly
- **Future-Ready**: Event-driven nature enables new business models (SaaS, marketplace)

Success depends on **committed cross-functional team**, **strong DevOps practices**, and **clear communication** around service boundaries and contracts. Following this roadmap and architecture principles will result in a platform competitive with modern veterinary software (VetTriage, ezyVet, Cornerstone).

---

**Document Version**: 1.0 | **Last Updated**: April 21, 2026 | **Author**: Solution Architecture Team
