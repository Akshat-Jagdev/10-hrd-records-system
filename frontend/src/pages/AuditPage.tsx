import { Fragment, useEffect, useState } from 'react'
import { apiError, searchAudit } from '../api'
import type { AuditEntry, PageResponse } from '../types'

const PAGE_SIZE = 20
const ENTITIES = ['UserAccount', 'Department', 'Employee', 'ProductionRecord', 'IncentiveScheme', 'IncentiveEarning']

/** datetime-local value → ISO instant (or undefined when empty). */
function toIso(localValue: string): string | undefined {
  if (!localValue) return undefined
  const date = new Date(localValue)
  return isNaN(date.getTime()) ? undefined : date.toISOString()
}

export default function AuditPage() {
  const [entity, setEntity] = useState('')
  const [usernameInput, setUsernameInput] = useState('')
  const [username, setUsername] = useState('')
  const [action, setAction] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)

  const [data, setData] = useState<PageResponse<AuditEntry> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadFlag, setReloadFlag] = useState(0)
  const [open, setOpen] = useState<Set<number>>(new Set())

  // Debounce the username filter.
  useEffect(() => {
    const timer = setTimeout(() => {
      setUsername(usernameInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [usernameInput])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    searchAudit({
      entity: entity || undefined,
      username: username || undefined,
      action: action || undefined,
      from: toIso(from),
      to: toIso(to),
      page,
      size: PAGE_SIZE,
    })
      .then((res) => {
        if (cancelled) return
        setData(res)
        setLoading(false)
        if (res.totalPages > 0 && page >= res.totalPages) setPage(res.totalPages - 1)
      })
      .catch((e) => {
        if (cancelled) return
        setError(apiError(e))
        setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [entity, username, action, from, to, page, reloadFlag])

  function toggle(id: number) {
    setOpen((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  const filtersActive = entity !== '' || username !== '' || action !== '' || from !== '' || to !== ''
  const rows = data?.content ?? []

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Audit trail</h1>
          <p className="muted">Who changed what, when — recorded automatically on every mutation</p>
        </div>
        <button className="btn btn-sm" onClick={() => setReloadFlag((n) => n + 1)}>
          Refresh
        </button>
      </div>

      <div className="filters">
        <div className="field">
          <label htmlFor="audit-entity">Entity</label>
          <select
            id="audit-entity"
            value={entity}
            onChange={(e) => {
              setEntity(e.target.value)
              setPage(0)
            }}
          >
            <option value="">All entities</option>
            {ENTITIES.map((en) => (
              <option key={en} value={en}>
                {en}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="audit-username">User</label>
          <input
            id="audit-username"
            value={usernameInput}
            onChange={(e) => setUsernameInput(e.target.value)}
            placeholder="hr.manager"
          />
        </div>
        <div className="field">
          <label htmlFor="audit-action">Action</label>
          <select
            id="audit-action"
            value={action}
            onChange={(e) => {
              setAction(e.target.value)
              setPage(0)
            }}
          >
            <option value="">All actions</option>
            <option value="CREATE">Create</option>
            <option value="UPDATE">Update</option>
            <option value="DELETE">Delete</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor="audit-from">From</label>
          <input
            id="audit-from"
            type="datetime-local"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value)
              setPage(0)
            }}
          />
        </div>
        <div className="field">
          <label htmlFor="audit-to">To</label>
          <input
            id="audit-to"
            type="datetime-local"
            value={to}
            onChange={(e) => {
              setTo(e.target.value)
              setPage(0)
            }}
          />
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}
      {loading && !data && <p className="muted">Loading audit trail...</p>}

      {data && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>User</th>
                  <th>Action</th>
                  <th>Entity</th>
                  <th aria-label="Details" />
                </tr>
              </thead>
              <tbody>
                {rows.map((a) => (
                  <Fragment key={a.id}>
                    <tr onClick={() => toggle(a.id)} style={{ cursor: 'pointer' }}>
                      <td>{new Date(a.occurredAt).toLocaleString('en-IN')}</td>
                      <td>{a.username}</td>
                      <td>{a.action}</td>
                      <td>
                        {a.entity} #{a.entityId}
                      </td>
                      <td className="muted">{open.has(a.id) ? 'Hide' : 'Details'}</td>
                    </tr>
                    {open.has(a.id) && (
                      <tr>
                        <td colSpan={5}>
                          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                            <div>
                              <div className="muted" style={{ marginBottom: 4 }}>
                                Before
                              </div>
                              <pre style={{ margin: 0, fontSize: 12, overflow: 'auto' }}>
                                {a.oldValue ? JSON.stringify(a.oldValue, null, 2) : '—'}
                              </pre>
                            </div>
                            <div>
                              <div className="muted" style={{ marginBottom: 4 }}>
                                After
                              </div>
                              <pre style={{ margin: 0, fontSize: 12, overflow: 'auto' }}>
                                {a.newValue ? JSON.stringify(a.newValue, null, 2) : '—'}
                              </pre>
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
                {rows.length === 0 && (
                  <tr>
                    <td colSpan={5} className="muted" style={{ textAlign: 'center' }}>
                      {filtersActive ? 'No audit entries match the current filters' : 'No changes recorded yet'}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          {data.totalPages > 0 && (
            <div className="pager">
              <button className="btn btn-sm" disabled={loading || page === 0} onClick={() => setPage((p) => p - 1)}>
                Prev
              </button>
              <span>
                Page {page + 1} of {data.totalPages} ({data.totalElements} total)
              </span>
              <button
                className="btn btn-sm"
                disabled={loading || page + 1 >= data.totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
