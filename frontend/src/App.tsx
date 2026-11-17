import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, RequireAuth, RequireRole } from './auth'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import EmployeesPage from './pages/EmployeesPage'
import DepartmentsPage from './pages/DepartmentsPage'
import ProductionPage from './pages/ProductionPage'
import IncentivesPage from './pages/IncentivesPage'
import AuditPage from './pages/AuditPage'
import AdminUsersPage from './pages/AdminUsersPage'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <RequireAuth>
              <Layout />
            </RequireAuth>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="employees" element={<EmployeesPage />} />
          <Route path="departments" element={<DepartmentsPage />} />
          <Route path="production" element={<ProductionPage />} />
          <Route path="incentives" element={<IncentivesPage />} />
          <Route
            path="audit"
            element={
              <RequireRole roles={['ADMIN', 'HR_MANAGER', 'AUDITOR']}>
                <AuditPage />
              </RequireRole>
            }
          />
          <Route
            path="admin/users"
            element={
              <RequireRole roles={['ADMIN']}>
                <AdminUsersPage />
              </RequireRole>
            }
          />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  )
}
