import { useState, type FormEvent, type MouseEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { apiError } from '../api'
import { useAuth } from '../auth'

const DEMO_ACCOUNTS = [
  { username: 'admin', password: 'Admin@HRD-Demo' },
  { username: 'hr.manager', password: 'HrManager@HRD-Demo' },
  { username: 'viewer', password: 'Viewer@HRD-Demo' },
  { username: 'auditor', password: 'Auditor@HRD-Demo' },
]

/** Centered sign-in card with clickable demo credentials. */
export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (user) return <Navigate to="/" replace />

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(username, password)
      navigate('/')
    } catch (err) {
      setError(apiError(err))
    } finally {
      setSubmitting(false)
    }
  }

  function fillCreds(account: (typeof DEMO_ACCOUNTS)[number], event: MouseEvent<HTMLTableRowElement>) {
    event.preventDefault()
    setUsername(account.username)
    setPassword(account.password)
    setError(null)
  }

  return (
    <div className="login-wrap">
      <div className="login-card">
        <div className="login-brand">
          <span className="brand-mark">HR</span>
          <span className="brand-name">HRD Records</span>
        </div>
        <h1>Sign in</h1>
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="login-username">Username</label>
            <input
              id="login-username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
            />
          </div>
          <div className="field">
            <label htmlFor="login-password">Password</label>
            <input
              id="login-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </div>
          {error && <p className="error-text">{error}</p>}
          <button className="btn btn-primary" type="submit" disabled={submitting} style={{ width: '100%' }}>
            {submitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
        <div className="demo-creds">
          <table>
            <tbody>
              {DEMO_ACCOUNTS.map((account) => (
                <tr
                  key={account.username}
                  onClick={(e) => fillCreds(account, e)}
                  style={{ cursor: 'pointer' }}
                  title={`Fill ${account.username} credentials`}
                >
                  <td>
                    <code>{account.username}</code>
                  </td>
                  <td>
                    <code>{account.password}</code>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <p>Demo environment. All data is synthetic.</p>
        </div>
      </div>
    </div>
  )
}
