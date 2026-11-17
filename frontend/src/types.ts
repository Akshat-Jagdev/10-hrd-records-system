// API types mirroring the backend DTOs (com.hrd.records.*Dtos).

export type Role = 'ADMIN' | 'HR_MANAGER' | 'VIEWER' | 'AUDITOR'
export type DepartmentType = 'PRODUCTION' | 'SUPPORT'
export type EmployeeStatus = 'ACTIVE' | 'RETIRED' | 'TRANSFERRED'
export type IncentiveStatus = 'DRAFT' | 'APPROVED' | 'REJECTED'
export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE'

export interface UserSummary {
  id: number
  username: string
  fullName: string
  role: Role
  enabled: boolean
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: UserSummary
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface DepartmentSummary {
  id: number
  code: string
  name: string
  type: DepartmentType
}

export interface EmployeeSummary {
  id: number
  empNo: string
  fullName: string
  grade: string
  designation: string
  department: DepartmentSummary
  doj: string
  status: EmployeeStatus
  basicPay: number
}

export interface ProductionSummary {
  id: number
  department: DepartmentSummary
  year: number
  month: number
  plannedTonnes: number
  actualTonnes: number
  achievementPct: number
}

export interface GradeRate {
  grade: string
  factor: number
}

export interface SchemeSummary {
  id: number
  name: string
  description: string | null
  thresholdPct: number
  ceilingPct: number
  ratePerPoint: number
  active: boolean
  effectiveFrom: string
  effectiveTo: string | null
  gradeRates: GradeRate[]
}

export interface EarningSummary {
  id: number
  employee: EmployeeSummary
  schemeId: number
  schemeName: string
  year: number
  month: number
  achievementPct: number
  amount: number
  status: IncentiveStatus
  decidedBy: string | null
  decidedAt: string | null
}

export interface CalculationResult {
  schemeId: number
  schemeName: string
  departmentId: number
  year: number
  month: number
  achievementPct: number
  draftCount: number
  totalDraftAmount: number
}

export interface AuditEntry {
  id: number
  occurredAt: string
  username: string
  action: AuditAction
  entity: string
  entityId: string
  oldValue: Record<string, unknown> | null
  newValue: Record<string, unknown> | null
}

export interface MonthlyProduction {
  year: number
  month: number
  plannedTonnes: number
  actualTonnes: number
  achievementPct: number
}

export interface DashboardSummary {
  activeEmployees: number
  totalEmployees: number
  departments: number
  productionDepartments: number
  pendingIncentives: number
  productionTrend: MonthlyProduction[]
  recentAudit: AuditEntry[]
}
