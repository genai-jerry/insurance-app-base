# Insurance CRM - Frontend

A modern React + TypeScript frontend for the Insurance CRM application with Material-UI components.

## Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **Material-UI (MUI)** - Component library
- **React Router v6** - Routing
- **TanStack Query (React Query)** - Data fetching and caching
- **Zustand** - State management
- **React Hook Form** - Form handling
- **Axios** - HTTP client
- **date-fns** - Date formatting

## Project Structure

```
src/
├── api/              # API client and endpoints
│   ├── client.ts     # Axios instance with interceptors
│   ├── auth.ts       # Authentication endpoints
│   ├── leads.ts      # Lead management endpoints
│   ├── products.ts   # Product and category endpoints
│   ├── voice.ts      # Voice session endpoints
│   ├── scheduler.ts  # Call scheduling endpoints
│   ├── prospectus.ts # Prospectus generation endpoints
│   ├── email.ts      # Email endpoints
│   └── admin.ts      # Admin endpoints
│
├── components/       # Reusable components
│   ├── Layout.tsx    # Main layout wrapper
│   ├── Navbar.tsx    # Top navigation bar
│   ├── Sidebar.tsx   # Side navigation
│   ├── PrivateRoute.tsx  # Protected route wrapper
│   ├── LoadingSpinner.tsx
│   └── ErrorAlert.tsx
│
├── pages/            # Page components
│   ├── auth/         # Authentication pages
│   │   └── Login.tsx
│   │
│   ├── agent/        # Agent UI pages
│   │   ├── Dashboard.tsx        # Today's calls and stats
│   │   ├── LeadList.tsx         # Lead table with filters
│   │   ├── LeadDetail.tsx       # Lead details and timeline
│   │   ├── LeadKanban.tsx       # Kanban board by status
│   │   ├── CallCalendar.tsx     # Call schedule
│   │   ├── ProductBrowser.tsx   # Browse products
│   │   ├── VoiceSessionViewer.tsx  # View voice transcripts
│   │   └── ProspectusPreview.tsx   # Preview/send prospectus
│   │
│   └── admin/        # Admin UI pages
│       ├── AdminDashboard.tsx   # Overview stats
│       ├── UserManagement.tsx   # CRUD users
│       ├── ProductManagement.tsx # CRUD products
│       ├── CategoryManagement.tsx # CRUD categories
│       ├── DocumentManagement.tsx # Upload/manage docs
│       ├── ModelConfig.tsx      # Configure AI models
│       └── AuditLogs.tsx        # View audit logs
│
├── store/            # State management
│   └── authStore.ts  # Zustand auth store
│
├── types/            # TypeScript types
│   └── index.ts      # All type definitions
│
├── utils/            # Utility functions
│   ├── formatters.ts # Date, currency, phone formatters
│   └── validators.ts # Form validation helpers
│
├── App.tsx           # Root component with routing
├── main.tsx          # App entry point
└── index.css         # Global styles
```

## Features

### Agent UI
- **Dashboard**: Today's call queue, lead stats, recent activity
- **Lead Management**:
  - List view with filters and search
  - Kanban board for visual workflow
  - Detailed lead view with timeline
  - Create/edit/assign leads
- **Call Scheduling**:
  - Calendar view of scheduled calls
  - Mark calls complete/cancelled
  - Add call notes
- **Products**: Browse and search insurance products
- **Voice Sessions**: View transcripts, extracted needs, and AI recommendations
- **Prospectus**: Generate and send product proposals

### Admin UI
- **Dashboard**: System-wide statistics and metrics
- **User Management**: Create/edit/delete users
- **Product Management**: CRUD operations for products
- **Category Management**: Manage product categories
- **Document Management**: Upload and organize documents by category
- **Model Configuration**: Configure OpenAI, Pinecone, and SMTP settings
- **Audit Logs**: Track all system activities

## Key Components

### API Client (`src/api/client.ts`)
- Axios instance with base URL `/api`
- Request interceptor adds JWT token to headers
- Response interceptor handles 401 errors (redirects to login)

### Auth Store (`src/store/authStore.ts`)
- Zustand store for authentication state
- Stores user, token, and authentication status
- Persists token in localStorage
- Methods: `login()`, `logout()`, `checkAuth()`

### Private Routes (`src/components/PrivateRoute.tsx`)
- Wrapper for protected routes
- Checks authentication status
- Supports role-based access (admin-only routes)
- Redirects to login if unauthenticated

## API Integration

All API calls go through the centralized API layer in `src/api/`:

```typescript
// Example: Fetching leads
import { leadsApi } from '@/api/leads';
import { useQuery } from '@tanstack/react-query';

const { data, isLoading, error } = useQuery({
  queryKey: ['leads'],
  queryFn: () => leadsApi.getAll({ page: 0, size: 10 }),
});
```

## Form Handling

Uses React Hook Form for form validation:

```typescript
import { useForm } from 'react-hook-form';

const { register, handleSubmit, formState: { errors } } = useForm();

const onSubmit = (data) => {
  // Handle form submission
};
```

## State Management

- **Global Auth State**: Zustand (`authStore.ts`)
- **Server State**: TanStack Query (caching, refetching)
- **Local State**: React useState

## Routing

Routes are organized by role:

- `/login` - Public login page
- `/agent/*` - Agent dashboard and features
- `/admin/*` - Admin panel (requires ADMIN role)

## Environment Variables

Create a `.env` file:

```env
VITE_API_URL=http://localhost:8080
```

## Development

```bash
# Install dependencies
npm install

# Start dev server (runs on port 3000)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run linter
npm run lint
```

## API Proxy

The Vite dev server proxies `/api/*` requests to the backend (configured in `vite.config.ts`):

```typescript
proxy: {
  '/api': {
    target: process.env.VITE_API_URL || 'http://localhost:8080',
    changeOrigin: true,
  },
}
```

## Authentication Flow

1. User submits login form
2. `authStore.login()` calls `/api/auth/login`
3. Backend returns JWT token and user object
4. Token stored in localStorage
5. User redirected to dashboard based on role
6. All subsequent API requests include token in `Authorization` header
7. On 401 error, user redirected to login and token cleared

## Type Safety

All API responses and request payloads are strongly typed using TypeScript interfaces defined in `src/types/index.ts`:

- User, Lead, Product, Category
- VoiceSession, Need, ProductRecommendation
- CallTask, ProspectusRequest, EmailLog
- AuditLog, Document, ModelConfig
- PageResponse<T> for paginated results

## Styling

- Material-UI theme customization in `App.tsx`
- Global styles in `index.css`
- Component-level styling using MUI's `sx` prop
- Responsive design with MUI Grid and breakpoints

## Error Handling

- API errors caught and displayed using `ErrorAlert` component
- React Query handles retry logic
- Loading states shown with `LoadingSpinner` component

## Future Enhancements

- Add real-time notifications using WebSockets
- Implement voice call integration
- Add data export functionality
- Enhanced analytics and reporting
- Dark mode support
- Internationalization (i18n)
