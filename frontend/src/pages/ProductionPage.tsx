import { useEffect, useState, type FormEvent } from 'react'
import {
  apiError,
  createProduction,
  deleteProduction,
  listDepartments,
  searchProduction,
  updateProduction,
} from '../api'
import { canWrite, useAuth } from '../auth'
import type { DepartmentSummary, PageResponse, ProductionSummary } from '../types'

const PAGE_SIZE = 20
const TONNES = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 })
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

interface ProductionFormState {
  departmentId: string
  year: string
  month: string
  plannedTonnes: string
  actualTonnes: string
}

const EMPTY_FORM: ProductionFormState = {
  departmentId: '',
  year: String(new Date().getFullYear()),
  month: '1',
  plannedTonnes: '',
  actualTonnes: '',
}

function formFromRecord(r: ProductionSummary): ProductionFormState {
  return {
    departmentId: String(r.department.id),
    year: String(r.year),
    month: String(r.month),
    plannedTonnes: String(r.plannedTonnes),
    actualTonnes: String(r.actualTonnes),
  }
}

export default function ProductionPage() {
  const { user } = useAuth()
  const writable = canWrite(user)

  const [departmentId, setDepartmentId] = useState('')
  const [year, setYear] = useState('')
  const [month, setMonth] = useState('')
  const [page, setPage] = useState(0)

  const [data, setData] = useState<PageResponse<ProductionSummary> | null>(null)
  const [departments, setDepartments] = useState<DepartmentSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadFlag, setReloadFlag] = useState(0)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<ProductionSummary | null>(null)
  const [form, setForm] = useState<ProductionFormState>(EMPTY_FORM)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let cancelled = false
    listDepartments()
      .then((deps) => {
        if (!cancelled) setDepartments(deps)
      })
      .catch(() => undefined)
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    searchProduction({
      departmentId: departmentId ? Number(departmentId) : undefined,
      year: year ? Number(year) : undefined,
      month: month ? Number(month) : undefined,
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
  }, [departmentId, year, month, page, reloadFlag])

  function openCreate() {
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormError('')
    setModalOpen(true)
  }

  function openEdit(record: ProductionSummary) {
    setEditing(record)
    setForm(formFromRecord(record))
    setFormError('')
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setFormError('')
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setFormError('')
    const body = {
      departmentId: Number(form.departmentId),
      year: Number(form.year),
      month: Number(form.month),
      plannedTonnes: Number(form.plannedTonnes),
      actualTonnes: Number(form.actualTonnes),
    }
    try {
      if (editing) {
        await updateProduction(editing.id, body)
      } else {
        await createProduction(body)
        setPage(0)
      }
      setModalOpen(false)
      setReloadFlag((n) => n + 1)
    } catch (e) {
      setFormError(apiError(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(record: ProductionSummary) {
    if (!window.confirm(`Delete the ${MONTHS[record.month - 1]} ${record.year} record for ${record.department.name}?`)) return
    try {
      await deleteProduction(record.id)
      setReloadFlag((n) => n + 1)
    } catch (e) {
      setError(apiError(e))
    }
  }

  const filtersActive = departmentId !== '' || year !== '' || month !== ''
  const rows = data?.content ?? []

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Production records</h1>
          <p className="muted">Monthly planned and actual tonnes per department</p>
        </div>
        {writable && (
          <button className="btn btn-primary" onClick={openCreate}>
            Add record
          </button>
        )}
      </div>

      <div className="filters">
        <div className="field">
          <label htmlFor="prod-department">Department</label>
          <select
            id="prod-department"
            value={departmentId}
            onChange={(e) => {
              setDepartmentId(e.target.value)
              setPage(0)
            }}
          >
            <option value="">All departments</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>
                {d.code} — {d.name}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="prod-year">Year</label>
          <input
            id="prod-year"
            type="number"
            min={2000}
            max={2100}
            value={year}
            onChange={(e) => {
              setYear(e.target.value)
              setPage(0)
            }}
          />
        </div>
        <div className="field">
          <label htmlFor="prod-month">Month</label>
          <select
            id="prod-month"
            value={month}
            onChange={(e) => {
              setMonth(e.target.value)
              setPage(0)
            }}
          >
            <option value="">All months</option>
            {MONTHS.map((m, i) => (
              <option key={m} value={i + 1}>
                {m}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}
      {loading && !data && <p className="muted">Loading production records...</p>}

      {data && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Department</th>
                  <th>Period</th>
                  <th className="num">Planned (t)</th>
                  <th className="num">Actual (t)</th>
                  <th className="num">Achievement</th>
                  {writable && <th aria-label="Actions" />}
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td>
                      {r.department.code} — {r.department.name}
                    </td>
                    <td>
                      {MONTHS[r.month - 1]} {r.year}
                    </td>
                    <td className="num">{TONNES.format(r.plannedTonnes)}</td>
                    <td className="num">{TONNES.format(r.actualTonnes)}</td>
                    <td className="num">
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8, justifyContent: 'flex-end' }}>
                        <span
                          aria-hidden="true"
                          style={{
                            display: 'inline-block',
                            width: 60,
                            height: 6,
                            borderRadius: 3,
                            background: 'var(--series-1-wash)',
                            overflow: 'hidden',
                          }}
                        >
                          <span
                            style={{
                              display: 'block',
                              height: '100%',
                              width: `${Math.min(r.achievementPct, 120) / 1.2}%`,
                              background: 'var(--series-1)',
                            }}
                          />
                        </span>
                        {r.achievementPct.toLocaleString('en-IN', { maximumFractionDigits: 1 })}%
                      </span>
                    </td>
                    {writable && (
                      <td>
                        <button className="btn btn-sm" onClick={() => openEdit(r)}>
                          Edit
                        </button>{' '}
                        <button className="btn btn-sm btn-danger" onClick={() => handleDelete(r)}>
                          Delete
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
                {rows.length === 0 && (
                  <tr>
                    <td colSpan={writable ? 6 : 5} className="muted" style={{ textAlign: 'center' }}>
                      {filtersActive ? 'No production records match the current filters' : 'No production records yet'}
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

      {modalOpen && (
        <div className="overlay">
          <div className="modal">
            <div className="modal-head">
              <h2>{editing ? `Edit ${MONTHS[Number(form.month) - 1]} ${form.year}` : 'Add production record'}</h2>
              <button className="btn btn-ghost" onClick={closeModal}>
                Close
              </button>
            </div>
            <form className="form-grid" onSubmit={handleSubmit}>
              <div className="field">
                <label htmlFor="prod-form-department">Department</label>
                <select
                  id="prod-form-department"
                  value={form.departmentId}
                  onChange={(e) => setForm({ ...form, departmentId: e.target.value })}
                  required
                >
                  <option value="">Select department</option>
                  {departments.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.code} — {d.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label htmlFor="prod-form-year">Year</label>
                <input
                  id="prod-form-year"
                  type="number"
                  min={2000}
                  max={2100}
                  value={form.year}
                  onChange={(e) => setForm({ ...form, year: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="prod-form-month">Month</label>
                <select
                  id="prod-form-month"
                  value={form.month}
                  onChange={(e) => setForm({ ...form, month: e.target.value })}
                  required
                >
                  {MONTHS.map((m, i) => (
                    <option key={m} value={i + 1}>
                      {m}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label htmlFor="prod-form-planned">Planned tonnes</label>
                <input
                  id="prod-form-planned"
                  type="number"
                  min={0.01}
                  step={0.01}
                  value={form.plannedTonnes}
                  onChange={(e) => setForm({ ...form, plannedTonnes: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="prod-form-actual">Actual tonnes</label>
                <input
                  id="prod-form-actual"
                  type="number"
                  min={0}
                  step={0.01}
                  value={form.actualTonnes}
                  onChange={(e) => setForm({ ...form, actualTonnes: e.target.value })}
                  required
                />
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                {formError && <p className="error-text">{formError}</p>}
                <div style={{ display: 'flex', gap: 8 }}>
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? 'Saving...' : editing ? 'Save changes' : 'Create record'}
                  </button>
                  <button type="button" className="btn" onClick={closeModal}>
                    Cancel
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
