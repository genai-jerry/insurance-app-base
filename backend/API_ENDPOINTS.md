# Insurance App - Complete API Endpoints Reference

## Authentication Required
All endpoints require JWT authentication via `Authorization: Bearer <token>` header unless otherwise noted.

## Role-Based Access Control
- `ADMIN` - Full access to all endpoints
- `AGENT` - Access to operational endpoints (leads, products, calls, voice, etc.)

---

## 1. AUTHENTICATION MODULE

### Auth Endpoints
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/auth/register` | Public | Register new user |
| POST | `/api/auth/login` | Public | Login and get JWT token |
| GET | `/api/auth/me` | Authenticated | Get current user profile |

---

## 2. LEADS MODULE

### Lead Management
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/leads` | ADMIN, AGENT | Create new lead |
| GET | `/api/leads` | ADMIN, AGENT | Get all leads (paginated) |
| GET | `/api/leads/{id}` | ADMIN, AGENT | Get lead by ID |
| PUT | `/api/leads/{id}` | ADMIN, AGENT | Update lead |
| DELETE | `/api/leads/{id}` | ADMIN | Delete lead |
| GET | `/api/leads/agent/{agentId}` | ADMIN, AGENT | Get leads by agent |
| GET | `/api/leads/status/{status}` | ADMIN, AGENT | Get leads by status |
| POST | `/api/leads/import` | ADMIN | Import leads from CSV |

### Lead Activities
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/leads/{leadId}/activities` | ADMIN, AGENT | Log activity |
| GET | `/api/leads/{leadId}/activities` | ADMIN, AGENT | Get lead activities |

---

## 3. PRODUCTS MODULE

### Product Management
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/products` | ADMIN | Create product |
| GET | `/api/products` | ADMIN, AGENT | Get all products |
| GET | `/api/products/{id}` | ADMIN, AGENT | Get product by ID |
| PUT | `/api/products/{id}` | ADMIN | Update product |
| DELETE | `/api/products/{id}` | ADMIN | Delete product |
| GET | `/api/products/category/{categoryId}` | ADMIN, AGENT | Get by category |
| GET | `/api/products/search` | ADMIN, AGENT | Search products |

### Product Categories
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/products/categories` | ADMIN | Create category |
| GET | `/api/products/categories` | ADMIN, AGENT | Get all categories |
| GET | `/api/products/categories/{id}` | ADMIN, AGENT | Get category by ID |
| PUT | `/api/products/categories/{id}` | ADMIN | Update category |
| DELETE | `/api/products/categories/{id}` | ADMIN | Delete category |

### Product Documents
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/products/{productId}/documents` | ADMIN | Upload document |
| GET | `/api/products/{productId}/documents` | ADMIN, AGENT | Get product docs |
| DELETE | `/api/products/documents/{id}` | ADMIN | Delete document |
| GET | `/api/products/documents/{id}/download` | ADMIN, AGENT | Download document |

---

## 4. SCHEDULER MODULE

### Call Task Management
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/scheduler/tasks` | ADMIN, AGENT | Create call task |
| GET | `/api/scheduler/tasks/{id}` | ADMIN, AGENT | Get task by ID |
| PUT | `/api/scheduler/tasks/{id}` | ADMIN, AGENT | Update task |
| DELETE | `/api/scheduler/tasks/{id}` | ADMIN, AGENT | Delete task |
| GET | `/api/scheduler/tasks/agent/{agentId}` | ADMIN, AGENT | Get tasks by agent |
| GET | `/api/scheduler/tasks/lead/{leadId}` | ADMIN, AGENT | Get tasks by lead |
| GET | `/api/scheduler/tasks/date-range` | ADMIN, AGENT | Get tasks in date range |
| GET | `/api/scheduler/tasks/agent/{agentId}/date-range` | ADMIN, AGENT | Get agent tasks in range |
| GET | `/api/scheduler/tasks/pending` | ADMIN, AGENT | Get pending tasks |
| POST | `/api/scheduler/tasks/{id}/complete` | ADMIN, AGENT | Mark task complete |

---

## 5. VOICE BOT MODULE

### Voice Sessions
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/voice/sessions/start` | ADMIN, AGENT | Start voice session |
| POST | `/api/voice/sessions/{id}/stop` | ADMIN, AGENT | Stop voice session |
| GET | `/api/voice/sessions/{id}` | ADMIN, AGENT | Get session details |
| GET | `/api/voice/sessions/lead/{leadId}` | ADMIN, AGENT | Get sessions by lead |
| GET | `/api/voice/sessions/agent/{agentId}` | ADMIN, AGENT | Get sessions by agent |
| GET | `/api/voice/sessions/{id}/transcript` | ADMIN, AGENT | Get transcript |
| PUT | `/api/voice/sessions/{id}/transcript` | ADMIN, AGENT | Update transcript |
| POST | `/api/voice/sessions/{id}/extract-needs` | ADMIN, AGENT | Extract needs from transcript |
| GET | `/api/voice/sessions/{id}/needs` | ADMIN, AGENT | Get extracted needs |
| GET | `/api/voice/sessions/{id}/recommendations` | ADMIN, AGENT | Get recommendations |

---

## 6. RAG MODULE

### Product Recommendations & Search
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/rag/recommend` | ADMIN, AGENT | Get product recommendations |
| POST | `/api/rag/reindex` | ADMIN | Re-index all content |
| POST | `/api/rag/reindex/products` | ADMIN | Re-index products only |
| POST | `/api/rag/reindex/documents` | ADMIN | Re-index documents only |
| GET | `/api/rag/search` | ADMIN, AGENT | Vector similarity search |

---

## 7. PROSPECTUS MODULE

### Prospectus Generation
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/prospectus/generate` | ADMIN, AGENT | Generate prospectus |
| GET | `/api/prospectus/{id}` | ADMIN, AGENT | Get prospectus details |
| GET | `/api/prospectus/lead/{leadId}` | ADMIN, AGENT | Get prospectuses by lead |
| GET | `/api/prospectus/{id}/html` | ADMIN, AGENT | Get HTML preview |
| GET | `/api/prospectus/{id}/download` | ADMIN, AGENT | Download PDF |
| GET | `/api/prospectus/{id}/preview` | ADMIN, AGENT | Preview PDF in browser |
| DELETE | `/api/prospectus/{id}` | ADMIN, AGENT | Delete prospectus |

---

## 8. EMAIL MODULE

### Email Management
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/email/send` | ADMIN, AGENT | Send email |
| POST | `/api/email/send-prospectus` | ADMIN, AGENT | Send prospectus email |
| GET | `/api/email/{id}` | ADMIN, AGENT | Get email details |
| GET | `/api/email/lead/{leadId}` | ADMIN, AGENT | Get email history for lead |
| GET | `/api/email/agent/{agentId}` | ADMIN, AGENT | Get emails by agent |
| GET | `/api/email/pending` | ADMIN | Get pending emails |

---

## 9. ADMIN MODULE

### Settings Management
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/admin/settings` | ADMIN | Get all settings |
| GET | `/api/admin/settings/{key}` | ADMIN | Get setting by key |
| POST | `/api/admin/settings` | ADMIN | Create setting |
| PUT | `/api/admin/settings/{key}` | ADMIN | Update setting |
| DELETE | `/api/admin/settings/{key}` | ADMIN | Delete setting |

### Audit Logs
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/admin/audit` | ADMIN | Get all audit logs (paginated) |
| GET | `/api/admin/audit/actor/{actorId}` | ADMIN | Get logs by user |
| GET | `/api/admin/audit/actor/{actorId}/page` | ADMIN | Get logs by user (paginated) |
| GET | `/api/admin/audit/entity/{entity}` | ADMIN | Get logs by entity type |
| GET | `/api/admin/audit/entity/{entity}/page` | ADMIN | Get logs by entity (paginated) |
| GET | `/api/admin/audit/entity/{entity}/{entityId}` | ADMIN | Get logs for entity instance |

### User Management
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/admin/users` | ADMIN | Get all users |
| GET | `/api/admin/users/{id}` | ADMIN | Get user by ID |
| POST | `/api/admin/users` | ADMIN | Create user |
| PUT | `/api/admin/users/{id}` | ADMIN | Update user |
| DELETE | `/api/admin/users/{id}` | ADMIN | Delete user |
| POST | `/api/admin/users/{id}/reset-password` | ADMIN | Reset user password |

---

## Request/Response Examples

### Start Voice Session
```http
POST /api/voice/sessions/start
Content-Type: application/json
Authorization: Bearer <token>

{
  "leadId": 1,
  "agentId": 2,
  "callTaskId": 5,
  "mockMode": true
}
```

Response:
```json
{
  "sessionId": 123,
  "openaiSessionId": "uuid-123",
  "status": "IN_PROGRESS",
  "message": "Voice session started",
  "websocketUrl": "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-12-17"
}
```

### Get Product Recommendations
```http
POST /api/rag/recommend
Content-Type: application/json
Authorization: Bearer <token>

{
  "query": "life insurance for family with young children",
  "customerNeeds": {
    "insuranceTypes": ["life", "health"],
    "coverageAmount": "$500,000",
    "budget": "$200/month"
  },
  "maxResults": 5,
  "voiceSessionId": 123
}
```

Response:
```json
{
  "narrative": "Based on your family situation with young children...",
  "products": [
    {
      "productId": 1,
      "productName": "Family Life Shield",
      "insurer": "ABC Insurance",
      "planType": "Term Life",
      "relevanceScore": 0.95,
      "reasoning": "This term life product matches...",
      "details": {...}
    }
  ],
  "matchedNeeds": {...}
}
```

### Generate Prospectus
```http
POST /api/prospectus/generate
Content-Type: application/json
Authorization: Bearer <token>

{
  "leadId": 1,
  "agentId": 2,
  "voiceSessionId": 123,
  "productIds": [1, 2, 3],
  "customerNeeds": {...},
  "additionalNotes": "Customer prefers monthly payments"
}
```

Response:
```json
{
  "id": 456,
  "leadId": 1,
  "leadName": "John Doe",
  "agentId": 2,
  "agentName": "Jane Agent",
  "voiceSessionId": 123,
  "version": 1,
  "htmlContent": "<!DOCTYPE html>...",
  "pdfPath": "/storage/prospectus/prospectus_456.pdf",
  "pdfUrl": "http://localhost:3000/api/prospectus/456/download",
  "createdAt": "2026-02-14T10:30:00"
}
```

### Send Prospectus Email
```http
POST /api/email/send-prospectus?leadId=1&agentId=2&prospectusId=456
Authorization: Bearer <token>
```

Response:
```json
{
  "id": 789,
  "leadId": 1,
  "leadName": "John Doe",
  "agentId": 2,
  "agentName": "Jane Agent",
  "prospectusId": 456,
  "toEmail": "john.doe@example.com",
  "subject": "Your Personalized Insurance Proposal - John Doe",
  "body": "Dear John Doe...",
  "status": "SENT",
  "providerMessageId": "msg-1234567890",
  "sentAt": "2026-02-14T10:35:00",
  "createdAt": "2026-02-14T10:35:00"
}
```

---

## Query Parameters

### Pagination
Most list endpoints support pagination:
- `page` - Page number (0-indexed)
- `size` - Items per page
- `sort` - Sort field and direction (e.g., `createdAt,desc`)

Example:
```
GET /api/leads?page=0&size=20&sort=createdAt,desc
```

### Date Range
Date range queries use ISO 8601 format:
```
GET /api/scheduler/tasks/date-range?startTime=2026-02-14T00:00:00&endTime=2026-02-21T23:59:59
```

### Search
Search endpoints support query parameters:
```
GET /api/products/search?query=life insurance&category=life&minPrice=100
```

---

## Error Responses

All endpoints return consistent error format:

```json
{
  "timestamp": "2026-02-14T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Lead ID is required",
  "path": "/api/voice/sessions/start"
}
```

Common HTTP Status Codes:
- `200 OK` - Success
- `201 Created` - Resource created
- `204 No Content` - Success with no response body
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Missing or invalid token
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## Swagger/OpenAPI Documentation

Interactive API documentation available at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

---

## Rate Limiting

Currently no rate limiting implemented. Recommended for production:
- 100 requests/minute per user for standard endpoints
- 10 requests/minute for AI-powered endpoints (recommendations, needs extraction)
- 5 requests/minute for resource-intensive operations (indexing, PDF generation)

---

## WebSocket Endpoints (Future)

Planned for real-time features:
- `/ws/voice/sessions/{sessionId}` - Real-time voice session updates
- `/ws/notifications` - Real-time notifications for agents
- `/ws/admin/monitor` - Admin monitoring dashboard

---

**Total Endpoints:** 70+ RESTful APIs
**Base URL:** `http://localhost:8080`
**Default Port:** 8080
**API Version:** v1 (implicit in path)
