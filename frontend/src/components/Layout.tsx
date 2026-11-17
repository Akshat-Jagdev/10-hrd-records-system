import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { canReadAudit, useAuth } from '../auth'

/** App shell: sidebar navigation (role-filtered) + header with the signed-in user. */
export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!user) return null

  const links: Array<{ to: string; label: string; show: boolean }> = [
    { to: '/', label: 'Dashboard', show: true },
    { to: '/employees', label: 'Employees', show: true },
    { to: '/departments', label: 'Departments', show: true },
    { to: '/production', label: 'Production', show: true },
    { to: '/incentives', label: 'Incentives', show: true },
    { to: '/audit', label: 'Audit trail', show: canReadAudit(user) },
    { to: '/admin/users', label: 'Users', show: user.role === 'ADMIN' },
  ]

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">HR</span>
          <span className="brand-name">HRD Records</span>
        </div>
        <nav>
          {links.filter((l) => l.show).map((l) => (
            <NavLink key={l.to} to={l.to} end={l.to === '/'}>
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot muted">Synthetic demo data</div>
      </aside>
      <div className="main">
        <header className="topbar">
          <div className="page-note muted">Steel plant HR &amp; production records</div>
          <div className="who">
            <span className="who-name">{user.fullName}</span>
            <span className="who-role">{user.role}</span>
            <button
              className="btn btn-ghost"
              onClick={() => {
                logout()
                navigate('/login')
              }}
            >
              Sign out
            </button>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
