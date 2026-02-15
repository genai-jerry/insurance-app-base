# Leads Management Module

## Overview
The Leads Management Module provides comprehensive functionality for managing insurance leads, including CRUD operations, lead assignment, status tracking, activity logging, and CSV import capabilities.

## Package Structure

```
com.insurance.leads/
├── controller/
│   └── LeadController.java          - REST API endpoints
├── dto/
│   ├── LeadDto.java                 - Lead data transfer object
│   ├── CreateLeadRequest.java       - Create lead request
│   ├── UpdateLeadRequest.java       - Update lead request
│   ├── LeadActivityDto.java         - Activity data transfer object
│   ├── CreateLeadActivityRequest.java - Create activity request
│   ├── LeadImportRequest.java       - CSV import request
│   └── LeadImportResponse.java      - CSV import response
├── exception/
│   └── LeadExceptionHandler.java    - Exception handling
├── mapper/
│   ├── LeadMapper.java              - MapStruct mapper for Lead
│   └── LeadActivityMapper.java      - MapStruct mapper for LeadActivity
├── repository/
│   ├── LeadRepository.java          - Lead data access layer
│   └── LeadActivityRepository.java  - Activity data access layer
└── service/
    ├── LeadService.java             - Lead business logic
    └── LeadActivityService.java     - Activity business logic
```

## Features

### 1. Lead Management
- **Create**: Create new leads with comprehensive information
- **Read**: Retrieve leads with filtering, pagination, and search
- **Update**: Update lead information
- **Delete**: Delete leads (admin only)
- **Assign**: Assign leads to agents
- **Status Update**: Update lead status (NEW, CONTACTED, QUALIFIED, PROPOSAL_SENT, CONVERTED, LOST)

### 2. Activity Tracking
- Log various activity types: NOTE, CALL, EMAIL, STATUS_CHANGE, PROSPECTUS_SENT
- View activity timeline for each lead
- Track all interactions with leads

### 3. CSV Import
- Bulk import leads from CSV files
- Optional default agent assignment
- Error handling for invalid data

### 4. Role-Based Access Control (RBAC)
- **Admins**: Full access to all leads and operations
- **Agents**: Can only view and update their assigned leads

## REST API Endpoints

### Lead Operations

#### Get All Leads
```http
GET /api/leads?page=0&size=10&sortBy=createdAt&sortDir=desc&status=NEW&search=john
```
**Query Parameters:**
- `agentId` (optional): Filter by assigned agent ID
- `status` (optional): Filter by lead status
- `search` (optional): Search by name, phone, or email
- `page` (default: 0): Page number
- `size` (default: 10): Page size
- `sortBy` (default: createdAt): Sort field
- `sortDir` (default: desc): Sort direction

**Authorization:** ADMIN or AGENT (agents see only their leads)

#### Get Lead by ID
```http
GET /api/leads/{id}
```
**Authorization:** ADMIN or AGENT (agents can only view their assigned leads)

#### Create Lead
```http
POST /api/leads
Content-Type: application/json

{
  "name": "John Doe",
  "phone": "+1234567890",
  "email": "john@example.com",
  "location": "New York",
  "age": 35,
  "incomeBand": "50K-75K",
  "leadSource": "Website",
  "status": "NEW",
  "assignedAgentId": 1,
  "timezone": "America/New_York",
  "preferredTimeWindows": {
    "morning": "9:00-12:00",
    "evening": "18:00-20:00"
  },
  "consentFlags": {
    "callConsent": true,
    "emailConsent": true
  },
  "notes": "Interested in life insurance"
}
```
**Authorization:** ADMIN only

#### Update Lead
```http
PUT /api/leads/{id}
Content-Type: application/json

{
  "status": "CONTACTED",
  "notes": "Called and left message"
}
```
**Authorization:** ADMIN or AGENT (agents can only update their assigned leads)

#### Delete Lead
```http
DELETE /api/leads/{id}
```
**Authorization:** ADMIN only

#### Assign Lead to Agent
```http
PUT /api/leads/{id}/assign?agentId=2
```
**Authorization:** ADMIN only

#### Update Lead Status
```http
PUT /api/leads/{id}/status?status=QUALIFIED
```
**Authorization:** ADMIN or AGENT (agents can only update their assigned leads)

### Activity Operations

#### Get Lead Activities
```http
GET /api/leads/{id}/activities
```
Returns activity timeline ordered by creation date (newest first).

**Authorization:** ADMIN or AGENT (agents can only view activities for their assigned leads)

#### Create Lead Activity
```http
POST /api/leads/{id}/activities
Content-Type: application/json

{
  "type": "NOTE",
  "payload": {
    "note": "Follow up scheduled for next week"
  }
}
```

**Activity Types:**
- `NOTE`: General notes
- `CALL`: Call logs
- `EMAIL`: Email communications
- `STATUS_CHANGE`: Status changes (usually auto-logged)
- `PROSPECTUS_SENT`: Prospectus delivery records

**Authorization:** ADMIN or AGENT (agents can only create activities for their assigned leads)

### Import Operations

#### Import Leads from CSV
```http
POST /api/leads/import
Content-Type: multipart/form-data

file: leads.csv
defaultAgentId: 1
```

**CSV Format:**
```csv
name,phone,email,location,age,income_band,lead_source,status,timezone,notes
John Doe,+1234567890,john@example.com,New York,35,50K-75K,Website,NEW,America/New_York,Interested in life insurance
Jane Smith,+0987654321,jane@example.com,Boston,28,75K-100K,Referral,NEW,America/New_York,Looking for health insurance
```

**Authorization:** ADMIN only

## RBAC Implementation

The module uses Spring Security's `@PreAuthorize` annotations for role-based access control:

### Admin Permissions
- Create leads
- Delete leads
- Assign leads to agents
- View all leads
- Update any lead
- Import leads from CSV

### Agent Permissions
- View only their assigned leads
- Update only their assigned leads
- Create activities for their assigned leads
- View activities for their assigned leads
- Update status of their assigned leads

### Implementation Example
```java
@GetMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
public ResponseEntity<LeadDto> getLeadById(@PathVariable Long id, Authentication authentication) {
    LeadDto lead = leadService.getLeadById(id);

    // RBAC: Agents can only view their own leads
    if (!isAdmin(authentication) && !lead.getAssignedAgentId().equals(getUserId(authentication))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    return ResponseEntity.ok(lead);
}
```

## Service Layer

### LeadService
Main service for lead operations:
- `getAllLeads()`: Get leads with filters and pagination
- `getLeadById()`: Get specific lead
- `createLead()`: Create new lead
- `updateLead()`: Update existing lead
- `deleteLead()`: Delete lead
- `assignLead()`: Assign lead to agent
- `updateLeadStatus()`: Update lead status
- `importLeadsFromCsv()`: Import from CSV using OpenCSV

### LeadActivityService
Service for activity tracking:
- `getLeadActivities()`: Get activity timeline
- `createLeadActivity()`: Create new activity
- `logNote()`: Convenience method for notes
- `logCall()`: Convenience method for calls
- `logEmail()`: Convenience method for emails
- `logStatusChange()`: Log status changes
- `logProspectusSent()`: Log prospectus delivery

## Repository Layer

### LeadRepository
Data access methods:
- `findByAssignedAgentId()`: Find leads by agent
- `findByStatus()`: Find leads by status
- `findByAssignedAgentIdAndStatus()`: Find by agent and status
- `searchLeads()`: Full-text search
- `searchLeadsByAgent()`: Search within agent's leads
- `searchLeadsByStatus()`: Search within status
- `countByStatus()`: Count leads by status

### LeadActivityRepository
Activity data access:
- `findByLeadIdOrderByCreatedAtDesc()`: Get activities timeline
- `findByLeadIdAndType()`: Get activities by type
- `countByLeadId()`: Count total activities
- `countByLeadIdAndType()`: Count activities by type

## Error Handling

The module includes comprehensive error handling via `LeadExceptionHandler`:
- `EntityNotFoundException`: 404 when lead not found
- `MethodArgumentNotValidException`: 400 for validation errors
- `IllegalArgumentException`: 400 for invalid arguments
- Generic exception handling for unexpected errors

## Dependencies

Required in `pom.xml`:
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Validation
- OpenCSV (already included)
- Lombok
- MapStruct
- PostgreSQL Driver

## Usage Examples

### Example 1: Create and Assign Lead
```java
// 1. Create lead (admin)
CreateLeadRequest request = CreateLeadRequest.builder()
    .name("John Doe")
    .phone("+1234567890")
    .email("john@example.com")
    .build();
LeadDto lead = leadService.createLead(request);

// 2. Assign to agent (admin)
leadService.assignLead(lead.getId(), agentId);

// 3. Agent updates status
leadService.updateLeadStatus(lead.getId(), LeadStatus.CONTACTED);

// 4. Agent logs activity
leadActivityService.logNote(lead.getId(), "Initial contact made");
```

### Example 2: Search and Filter Leads
```java
// Agent gets their NEW leads
Page<LeadDto> newLeads = leadService.getAllLeads(
    agentId,                    // agentId
    LeadStatus.NEW,            // status
    null,                      // search
    PageRequest.of(0, 10)     // pageable
);

// Admin searches all leads
Page<LeadDto> searchResults = leadService.getAllLeads(
    null,                      // agentId (null = all)
    null,                      // status (null = all)
    "john",                    // search term
    PageRequest.of(0, 20)     // pageable
);
```

### Example 3: Import Leads
```java
// Admin imports CSV
List<LeadDto> imported = leadService.importLeadsFromCsv(
    csvFile,
    defaultAgentId  // Optional: assign all imported leads to this agent
);
```

## Testing

The module is ready for testing with:
- Unit tests for services
- Integration tests for repositories
- Controller tests for REST endpoints
- Security tests for RBAC

## Next Steps

To integrate with the authentication module:
1. Update `getUserId()` method in `LeadController` to extract user ID from your authentication implementation
2. Ensure your authentication filter populates the `Authentication` object correctly
3. Configure method security with `@EnableMethodSecurity` in your security configuration

## Notes

- All timestamps are automatically managed by Hibernate (@CreationTimestamp, @UpdateTimestamp)
- JSONB fields (preferredTimeWindows, consentFlags) are stored as PostgreSQL JSONB
- CSV import supports flexible column ordering
- All operations are transactional
- Logging is implemented using SLF4J
