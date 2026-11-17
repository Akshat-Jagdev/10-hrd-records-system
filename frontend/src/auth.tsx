import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { login as apiLogin } from './api'
import type { Role, UserSummary } from './types'

const TOKEN_KEY = 'hrd_token'
const USER_KEY = 'hrd_user'

export function storedToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}
function storedUser(): UserSummary | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserSummary
  } catch {
    return null
  }
}

interface AuthContextValue {
  user: UserSummary | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(storedUser)

  const login = useCallback(async (username: string, password: string) => {
    const result = await apiLogin(username, password)
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
    setUser(result.user)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }, [])

  const value = useMemo(() => ({ user, login, logout }), [user, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}

/** ADMIN and HR_MANAGER may write business data. */
export function canWrite(user: UserSummary | null): boolean {
  return user?.role === 'ADMIN' || user?.role === 'HR_MANAGER'
}

/** Audit trail is visible to ADMIN, HR_MANAGER and AUDITOR. */
export function canReadAudit(user: UserSummary | null): boolean {
  return user?.role === 'ADMIN' || user?.role === 'HR_MANAGER' || user?.role === 'AUDITOR'
}

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

export function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (!roles.includes(user.role)) {
    return (
      <div className="card forbidden">
        <h2>Not available for your role</h2>
        <p className="muted">Signed in as {user.fullName} ({user.role}). This section requires: {roles.join(' or ')}.</p>
      </div>
    )
  }
  return <>{children}</>
}
