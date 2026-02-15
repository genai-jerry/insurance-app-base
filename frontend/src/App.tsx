import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import { useAuthStore } from './store/authStore';
import { PrivateRoute } from './components/PrivateRoute';
import { Layout } from './components/Layout';

// Auth Pages
import { Login } from './pages/auth/Login';
import { ForgotPassword } from './pages/auth/ForgotPassword';
import { ResetPassword } from './pages/auth/ResetPassword';

// Agent Pages
import { Dashboard } from './pages/agent/Dashboard';
import { LeadList } from './pages/agent/LeadList';
import { LeadDetail } from './pages/agent/LeadDetail';
import { LeadKanban } from './pages/agent/LeadKanban';
import { CallCalendar } from './pages/agent/CallCalendar';
import { ProductBrowser } from './pages/agent/ProductBrowser';
import { VoiceSessionViewer } from './pages/agent/VoiceSessionViewer';
import { ProspectusPreview } from './pages/agent/ProspectusPreview';

// Admin Pages
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { UserManagement } from './pages/admin/UserManagement';
import { ProductManagement } from './pages/admin/ProductManagement';
import { CategoryManagement } from './pages/admin/CategoryManagement';
import { DocumentManagement } from './pages/admin/DocumentManagement';
import { ModelConfig } from './pages/admin/ModelConfig';
import { AuditLogs } from './pages/admin/AuditLogs';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
  },
});

function App() {
  const { checkAuth, isAuthenticated } = useAuthStore();

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <BrowserRouter>
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />

            {/* Agent Routes */}
            <Route
              path="/agent/*"
              element={
                <PrivateRoute>
                  <Layout>
                    <Routes>
                      <Route path="dashboard" element={<Dashboard />} />
                      <Route path="leads" element={<LeadList />} />
                      <Route path="leads/:id" element={<LeadDetail />} />
                      <Route path="kanban" element={<LeadKanban />} />
                      <Route path="calendar" element={<CallCalendar />} />
                      <Route path="products" element={<ProductBrowser />} />
                      <Route path="voice-sessions/:sessionId" element={<VoiceSessionViewer />} />
                      <Route path="prospectus/:requestId" element={<ProspectusPreview />} />
                      <Route
                        path="*"
                        element={<Navigate to="/agent/dashboard" replace />}
                      />
                    </Routes>
                  </Layout>
                </PrivateRoute>
              }
            />

            {/* Admin Routes */}
            <Route
              path="/admin/*"
              element={
                <PrivateRoute requireAdmin>
                  <Layout>
                    <Routes>
                      <Route path="dashboard" element={<AdminDashboard />} />
                      <Route path="users" element={<UserManagement />} />
                      <Route path="products" element={<ProductManagement />} />
                      <Route path="categories" element={<CategoryManagement />} />
                      <Route path="documents" element={<DocumentManagement />} />
                      <Route path="config" element={<ModelConfig />} />
                      <Route path="audit" element={<AuditLogs />} />
                      <Route
                        path="*"
                        element={<Navigate to="/admin/dashboard" replace />}
                      />
                    </Routes>
                  </Layout>
                </PrivateRoute>
              }
            />

            {/* Default Route */}
            <Route
              path="/"
              element={
                isAuthenticated ? (
                  <Navigate to="/agent/dashboard" replace />
                ) : (
                  <Navigate to="/login" replace />
                )
              }
            />

            {/* Catch all */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </ThemeProvider>
    </QueryClientProvider>
  );
}

export default App;
