import axios from 'axios'
import type {
  AuditEntry,
  CalculationResult,
  DashboardSummary,
  DepartmentSummary,
  EarningSummary,
  EmployeeSummary,
  IncentiveStatus,
  LoginResponse,
  PageResponse,
  ProductionSummary,
  SchemeSummary,
  UserSummary,
} from './types'

const TOKEN_KEY = 'hrd_token'

export const api = axios.create({ baseURL: '/api/v1' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// A rejected token (expired/invalid) drops the session and returns to login.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && localStorage.getItem(TOKEN_KEY)) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('hrd_user')
      window.location.assign('/login')
    }
    return Promise.reject(error)
  },
)

/** Extracts the RFC 7807 `detail` the backend returns on errors. */
export function apiError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const detail = error.response?.data?.detail
    if (typeof detail === 'string' && detail) return detail
    if (Array.isArray(detail)) return detail.join('; ')
    return error.message
  }
  return String(error)
}

// ---- auth ----
export async function login(username: string, password: string): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/auth/login', { username, password })
  return data
}
export async function getMe(): Promise<UserSummary> {
  const { data } = await api.get<UserSummary>('/auth/me')
  return data
}

// ---- admin: users (ADMIN) ----
export async function listUsers(): Promise<UserSummary[]> {
  const { data } = await api.get<UserSummary[]>('/admin/users')
  return data
}
export async function createUser(body: { username: string; password: string; fullName: string; role: string }): Promise<UserSummary> {
  const { data } = await api.post<UserSummary>('/admin/users', body)
  return data
}
export async function updateUser(id: number, body: { fullName: string; role: string; enabled: boolean }): Promise<UserSummary> {
  const { data } = await api.put<UserSummary>(`/admin/users/${id}`, body)
  return data
}
export async function deleteUser(id: number): Promise<void> {
  await api.delete(`/admin/users/${id}`)
}

// ---- departments ----
export async function listDepartments(): Promise<DepartmentSummary[]> {
  const { data } = await api.get<DepartmentSummary[]>('/departments')
  return data
}
export async function createDepartment(body: { code: string; name: string; type: string }): Promise<DepartmentSummary> {
  const { data } = await api.post<DepartmentSummary>('/departments', body)
  return data
}
export async function updateDepartment(id: number, body: { code: string; name: string; type: string }): Promise<DepartmentSummary> {
  const { data } = await api.put<DepartmentSummary>(`/departments/${id}`, body)
  return data
}
export async function deleteDepartment(id: number): Promise<void> {
  await api.delete(`/departments/${id}`)
}

// ---- employees ----
export interface EmployeeFilters {
  empNo?: string
  name?: string
  departmentId?: number
  status?: string
  page?: number
  size?: number
}
export async function searchEmployees(filters: EmployeeFilters): Promise<PageResponse<EmployeeSummary>> {
  const { data } = await api.get<PageResponse<EmployeeSummary>>('/employees', { params: filters })
  return data
}
export async function createEmployee(body: Record<string, unknown>): Promise<EmployeeSummary> {
  const { data } = await api.post<EmployeeSummary>('/employees', body)
  return data
}
export async function updateEmployee(id: number, body: Record<string, unknown>): Promise<EmployeeSummary> {
  const { data } = await api.put<EmployeeSummary>(`/employees/${id}`, body)
  return data
}
export async function deleteEmployee(id: number): Promise<void> {
  await api.delete(`/employees/${id}`)
}

// ---- production ----
export interface ProductionFilters {
  departmentId?: number
  year?: number
  month?: number
  page?: number
  size?: number
}
export async function searchProduction(filters: ProductionFilters): Promise<PageResponse<ProductionSummary>> {
  const { data } = await api.get<PageResponse<ProductionSummary>>('/production', { params: filters })
  return data
}
export async function createProduction(body: Record<string, unknown>): Promise<ProductionSummary> {
  const { data } = await api.post<ProductionSummary>('/production', body)
  return data
}
export async function updateProduction(id: number, body: Record<string, unknown>): Promise<ProductionSummary> {
  const { data } = await api.put<ProductionSummary>(`/production/${id}`, body)
  return data
}
export async function deleteProduction(id: number): Promise<void> {
  await api.delete(`/production/${id}`)
}

// ---- incentives ----
export async function listSchemes(): Promise<SchemeSummary[]> {
  const { data } = await api.get<SchemeSummary[]>('/incentives/schemes')
  return data
}
export async function createScheme(body: Record<string, unknown>): Promise<SchemeSummary> {
  const { data } = await api.post<SchemeSummary>('/incentives/schemes', body)
  return data
}
export async function updateScheme(id: number, body: Record<string, unknown>): Promise<SchemeSummary> {
  const { data } = await api.put<SchemeSummary>(`/incentives/schemes/${id}`, body)
  return data
}
export async function deleteScheme(id: number): Promise<void> {
  await api.delete(`/incentives/schemes/${id}`)
}
export interface EarningFilters {
  employeeId?: number
  schemeId?: number
  year?: number
  month?: number
  status?: IncentiveStatus
  page?: number
  size?: number
}
export async function searchEarnings(filters: EarningFilters): Promise<PageResponse<EarningSummary>> {
  const { data } = await api.get<PageResponse<EarningSummary>>('/incentives/earnings', { params: filters })
  return data
}
export async function calculateEarnings(body: { schemeId: number; departmentId: number; year: number; month: number }): Promise<CalculationResult> {
  const { data } = await api.post<CalculationResult>('/incentives/earnings/calculate', body)
  return data
}
export async function approveEarning(id: number): Promise<EarningSummary> {
  const { data } = await api.post<EarningSummary>(`/incentives/earnings/${id}/approve`)
  return data
}
export async function rejectEarning(id: number): Promise<EarningSummary> {
  const { data } = await api.post<EarningSummary>(`/incentives/earnings/${id}/reject`)
  return data
}

// ---- audit (ADMIN / HR_MANAGER / AUDITOR) ----
export interface AuditFilters {
  entity?: string
  username?: string
  action?: string
  from?: string
  to?: string
  page?: number
  size?: number
}
export async function searchAudit(filters: AuditFilters): Promise<PageResponse<AuditEntry>> {
  const { data } = await api.get<PageResponse<AuditEntry>>('/audit', { params: filters })
  return data
}

// ---- dashboard ----
export async function getDashboard(): Promise<DashboardSummary> {
  const { data } = await api.get<DashboardSummary>('/dashboard')
  return data
}
