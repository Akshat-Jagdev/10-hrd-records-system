import { useEffect, useState } from 'react'
import { apiError, getDashboard } from '../api'
import TrendChart from '../components/TrendChart'
import type { DashboardSummary } from '../types'

export default function DashboardPage() {
  const [data, setData] = useState<DashboardSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    getDashboard()
      .then((res) => {
        if (cancelled) return
        setData(res)
        setLoading(false)
      })
      .catch((e) => {
        if (cancelled) return
        setError(apiError(e))
        setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  if (loading) return <p className="muted">Loading dashboard...</p>
  if (error) return <p className="error-text">{error}</p>
  if (!data) return null

  const latest = data.productionTrend[data.productionTrend.length - 1]

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Dashboard</h1>
          <p className="muted">Plant overview — synthetic demo data</p>
        </div>
      </div>

      <div className="tile-row">
        <div className="tile">
          <div className="tile-label">Active employees</div>
          <div className="tile-value">{data.activeEmployees.toLocaleString('en-IN')}</div>
          <div className="tile-sub">of {data.totalEmployees.toLocaleString('en-IN')} total</div>
        </div>
        <div className="tile">
          <div className="tile-label">Departments</div>
          <div className="tile-value">{data.departments}</div>
          <div className="tile-sub">{data.productionDepartments} production shops</div>
        </div>
        <div className="tile">
          <div className="tile-label">Pending incentive drafts</div>
          <div className="tile-value">{data.pendingIncentives.toLocaleString('en-IN')}</div>
          <div className="tile-sub">awaiting decision</div>
        </div>
        <div className="tile">
          <div className="tile-label">Latest achievement</div>
          <div className="tile-value">{latest ? `${latest.achievementPct.toLocaleString('en-IN', { maximumFractionDigits: 1 })}%` : '—'}</div>
          <div className="tile-sub">{latest ? 'latest month vs plan' : 'no production data'}</div>
        </div>
      </div>

      <div className="trend-card">
        <div className="trend-head">
          <div>
            <h2>Production vs plan</h2>
            <p className="muted" style={{ margin: 0 }}>
              tonnes per month, all production departments combined
            </p>
          </div>
          {/* legend: two series, identity never by color alone */}
          <div className="trend-legend">
            <span className="key">
              <span className="key-swatch" style={{ background: 'var(--series-1)' }} />
              Planned
            </span>
            <span className="key">
              <span className="key-swatch" style={{ background: 'var(--series-2)' }} />
              Actual
            </span>
          </div>
        </div>
        <TrendChart data={data.productionTrend} />
      </div>

      <div className="card">
        <h2>Recent activity</h2>
        {data.recentAudit.length === 0 ? (
          <p className="muted">No activity recorded yet. Changes you make will appear here.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>User</th>
                  <th>Action</th>
                  <th>Entity</th>
                </tr>
              </thead>
              <tbody>
                {data.recentAudit.map((a) => (
                  <tr key={a.id}>
                    <td>{new Date(a.occurredAt).toLocaleString('en-IN')}</td>
                    <td>{a.username}</td>
                    <td>{a.action}</td>
                    <td>
                      {a.entity} #{a.entityId}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
