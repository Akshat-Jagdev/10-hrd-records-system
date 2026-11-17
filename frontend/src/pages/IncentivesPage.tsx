import { useEffect, useState, type FormEvent } from 'react'
import {
  apiError,
  approveEarning,
  calculateEarnings,
  createScheme,
  deleteScheme,
  listDepartments,
  listSchemes,
  rejectEarning,
  searchEarnings,
  updateScheme,
} from '../api'
import { canWrite, useAuth } from '../auth'
import StatusBadge from '../components/StatusBadge'
import type {
  CalculationResult,
  DepartmentSummary,
  EarningSummary,
  IncentiveStatus,
  PageResponse,
  SchemeSummary,
} from '../types'

const PAGE_SIZE = 20
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
const GRADES = ['E0', 'E1', 'E2', 'E3', 'E4', 'E5', 'M1', 'M2']
const PCT = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 1 })
const INR = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })

interface GradeRateRow {
  grade: string
  factor: string
}

interface SchemeFormState {
  name: string
  description: string
  thresholdPct: string
  ceilingPct: string
  ratePerPoint: string
  active: boolean
  effectiveFrom: string
  effectiveTo: string
  gradeRates: GradeRateRow[]
}

const EMPTY_SCHEME: SchemeFormState = {
  name: '',
  description: '',
  thresholdPct: '95',
  ceilingPct: '115',
  ratePerPoint: '850',
  active: true,
  effectiveFrom: '',
  effectiveTo: '',
  gradeRates: GRADES.map((g) => ({ grade: g, factor: '1.00' })),
}

function formFromScheme(s: SchemeSummary): SchemeFormState {
  return {
    name: s.name,
    description: s.description ?? '',
    thresholdPct: String(s.thresholdPct),
    ceilingPct: String(s.ceilingPct),
    ratePerPoint: String(s.ratePerPoint),
    active: s.active,
    effectiveFrom: s.effectiveFrom.slice(0, 10),
    effectiveTo: s.effectiveTo ? s.effectiveTo.slice(0, 10) : '',
    gradeRates: s.gradeRates.map((r) => ({ grade: r.grade, factor: String(r.factor) })),
  }
}

function periodRange(from: string, to: string | null): string {
  const f = from ? `${MONTHS[Number(from.slice(5, 7)) - 1]} ${from.slice(0, 4)}` : ''
  if (!to) return f ? `since ${f}` : ''
  return `${f} - ${MONTHS[Number(to.slice(5, 7)) - 1]} ${to.slice(0, 4)}`
}

export default function IncentivesPage() {
  const { user } = useAuth()
  const writable = canWrite(user)

  // schemes
  const [schemes, setSchemes] = useState<SchemeSummary[]>([])
  const [departments, setDepartments] = useState<DepartmentSummary[]>([])
  const [schemesError, setSchemesError] = useState('')

  // scheme modal
  const [schemeModalOpen, setSchemeModalOpen] = useState(false)
  const [editingScheme, setEditingScheme] = useState<SchemeSummary | null>(null)
  const [schemeForm, setSchemeForm] = useState<SchemeFormState>(EMPTY_SCHEME)
  const [schemeFormError, setSchemeFormError] = useState('')
  const [savingScheme, setSavingScheme] = useState(false)

  // calculate panel
  const [calcSchemeId, setCalcSchemeId] = useState('')
  const [calcDeptId, setCalcDeptId] = useState('')
  const [calcYear, setCalcYear] = useState(String(new Date().getFullYear()))
  const [calcMonth, setCalcMonth] = useState('1')
  const [calcResult, setCalcResult] = useState<CalculationResult | null>(null)
  const [calcError, setCalcError] = useState('')
  const [calculating, setCalculating] = useState(false)

  // earnings table
  const [filterSchemeId, setFilterSchemeId] = useState('')
  const [filterStatus, setFilterStatus] = useState('')
  const [filterYear, setFilterYear] = useState('')
  const [filterMonth, setFilterMonth] = useState('')
  const [page, setPage] = useState(0)
  const [earnings, setEarnings] = useState<PageResponse<EarningSummary> | null>(null)
  const [earningsLoading, setEarningsLoading] = useState(true)
  const [earningsError, setEarningsError] = useState('')
  const [reloadFlag, setReloadFlag] = useState(0)

  useEffect(() => {
    let cancelled = false
    listSchemes()
      .then((s) => !cancelled && setSchemes(s))
      .catch((e) => !cancelled && setSchemesError(apiError(e)))
    listDepartments()
      .then((d) => !cancelled && setDepartments(d))
      .catch(() => undefined)
    return () => {
      cancelled = true
    }
  }, [reloadFlag])

  useEffect(() => {
    let cancelled = false
    setEarningsLoading(true)
    setEarningsError('')
    searchEarnings({
      schemeId: filterSchemeId ? Number(filterSchemeId) : undefined,
      status: (filterStatus || undefined) as IncentiveStatus | undefined,
      year: filterYear ? Number(filterYear) : undefined,
      month: filterMonth ? Number(filterMonth) : undefined,
      page,
      size: PAGE_SIZE,
    })
      .then((res) => {
        if (cancelled) return
        setEarnings(res)
        setEarningsLoading(false)
        if (res.totalPages > 0 && page >= res.totalPages) setPage(res.totalPages - 1)
      })
      .catch((e) => {
        if (cancelled) return
        setEarningsError(apiError(e))
        setEarningsLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [filterSchemeId, filterStatus, filterYear, filterMonth, page, reloadFlag])

  function reloadAll() {
    setReloadFlag((n) => n + 1)
  }

  // ---------- scheme modal ----------

  function openSchemeCreate() {
    setEditingScheme(null)
    setSchemeForm(EMPTY_SCHEME)
    setSchemeFormError('')
    setSchemeModalOpen(true)
  }

  function openSchemeEdit(scheme: SchemeSummary) {
    setEditingScheme(scheme)
    setSchemeForm(formFromScheme(scheme))
    setSchemeFormError('')
    setSchemeModalOpen(true)
  }

  async function handleSchemeSubmit(event: FormEvent) {
    event.preventDefault()
    setSavingScheme(true)
    setSchemeFormError('')
    const body = {
      name: schemeForm.name.trim(),
      description: schemeForm.description.trim() || null,
      thresholdPct: Number(schemeForm.thresholdPct),
      ceilingPct: Number(schemeForm.ceilingPct),
      ratePerPoint: Number(schemeForm.ratePerPoint),
      active: schemeForm.active,
      effectiveFrom: schemeForm.effectiveFrom,
      effectiveTo: schemeForm.effectiveTo || null,
      gradeRates: schemeForm.gradeRates
        .filter((r) => r.grade)
        .map((r) => ({ grade: r.grade, factor: Number(r.factor) })),
    }
    try {
      if (editingScheme) {
        await updateScheme(editingScheme.id, body)
      } else {
        await createScheme(body)
      }
      setSchemeModalOpen(false)
      reloadAll()
    } catch (e) {
      setSchemeFormError(apiError(e))
    } finally {
      setSavingScheme(false)
    }
  }

  async function handleSchemeDelete(scheme: SchemeSummary) {
    if (!window.confirm(`Delete scheme "${scheme.name}"?`)) return
    try {
      await deleteScheme(scheme.id)
      reloadAll()
    } catch (e) {
      setSchemesError(apiError(e))
    }
  }

  function setRate(index: number, patch: Partial<GradeRateRow>) {
    setSchemeForm((f) => ({
      ...f,
      gradeRates: f.gradeRates.map((r, i) => (i === index ? { ...r, ...patch } : r)),
    }))
  }

  // ---------- calculate ----------

  async function handleCalculate(event: FormEvent) {
    event.preventDefault()
    setCalculating(true)
    setCalcError('')
    setCalcResult(null)
    try {
      const result = await calculateEarnings({
        schemeId: Number(calcSchemeId),
        departmentId: Number(calcDeptId),
        year: Number(calcYear),
        month: Number(calcMonth),
      })
      setCalcResult(result)
      reloadAll()
    } catch (e) {
      setCalcError(apiError(e))
    } finally {
      setCalculating(false)
    }
  }

  // ---------- approve / reject ----------

  async function handleDecide(earning: EarningSummary, action: 'approve' | 'reject') {
    const verb = action === 'approve' ? 'Approve' : 'Reject'
    if (!window.confirm(`${verb} the draft incentive of ${INR.format(earning.amount)} for ${earning.employee.fullName}?`)) return
    try {
      if (action === 'approve') {
        await approveEarning(earning.id)
      } else {
        await rejectEarning(earning.id)
      }
      reloadAll()
    } catch (e) {
      setEarningsError(apiError(e))
    }
  }

  const productionDepartments = departments.filter((d) => d.type === 'PRODUCTION')
  const activeSchemes = schemes.filter((s) => s.active)
  const earningRows = earnings?.content ?? []
  const filtersActive = filterSchemeId !== '' || filterStatus !== '' || filterYear !== '' || filterMonth !== ''

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Incentives</h1>
          <p className="muted">Schemes, draft calculation and approval decisions</p>
        </div>
        {writable && (
          <button className="btn btn-primary" onClick={openSchemeCreate}>
            Add scheme
          </button>
        )}
      </div>

      {/* ---------- schemes ---------- */}
      <div className="card">
        <h2>Schemes</h2>
        {schemesError && <p className="error-text">{schemesError}</p>}
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Active</th>
                <th className="num">Threshold</th>
                <th className="num">Ceiling</th>
                <th className="num">Rate / point</th>
                <th>Effective</th>
                <th>Grade rates</th>
                {writable && <th aria-label="Actions" />}
              </tr>
            </thead>
            <tbody>
              {schemes.map((s) => (
                <tr key={s.id}>
                  <td>{s.name}</td>
                  <td>
                    <StatusBadge status={s.active} />
                  </td>
                  <td className="num">{PCT.format(s.thresholdPct)}%</td>
                  <td className="num">{PCT.format(s.ceilingPct)}%</td>
                  <td className="num">{INR.format(s.ratePerPoint)}</td>
                  <td>{periodRange(s.effectiveFrom, s.effectiveTo)}</td>
                  <td className="muted">{s.gradeRates.length} grades</td>
                  {writable && (
                    <td>
                      <button className="btn btn-sm" onClick={() => openSchemeEdit(s)}>
                        Edit
                      </button>{' '}
                      <button className="btn btn-sm btn-danger" onClick={() => handleSchemeDelete(s)}>
                        Delete
                      </button>
                    </td>
                  )}
                </tr>
              ))}
              {schemes.length === 0 && (
                <tr>
                  <td colSpan={writable ? 8 : 7} className="muted" style={{ textAlign: 'center' }}>
                    No incentive schemes yet
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ---------- calculate ---------- */}
      {writable && (
        <div className="card">
          <h2>Calculate draft earnings</h2>
          <p className="muted">
            Recomputes drafts for one scheme + department + period from the production record. Existing drafts for that
            cell are replaced; approved history is untouched.
          </p>
          <form className="filters" onSubmit={handleCalculate}>
            <div className="field">
              <label htmlFor="calc-scheme">Scheme</label>
              <select id="calc-scheme" value={calcSchemeId} onChange={(e) => setCalcSchemeId(e.target.value)} required>
                <option value="">Select scheme</option>
                {activeSchemes.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="calc-dept">Department</label>
              <select id="calc-dept" value={calcDeptId} onChange={(e) => setCalcDeptId(e.target.value)} required>
                <option value="">Select department</option>
                {productionDepartments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.code} — {d.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="calc-year">Year</label>
              <input
                id="calc-year"
                type="number"
                min={2000}
                max={2100}
                value={calcYear}
                onChange={(e) => setCalcYear(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="calc-month">Month</label>
              <select id="calc-month" value={calcMonth} onChange={(e) => setCalcMonth(e.target.value)} required>
                {MONTHS.map((m, i) => (
                  <option key={m} value={i + 1}>
                    {m}
                  </option>
                ))}
              </select>
            </div>
            <button type="submit" className="btn btn-primary" disabled={calculating}>
              {calculating ? 'Calculating...' : 'Calculate'}
            </button>
          </form>
          {calcError && <p className="error-text">{calcError}</p>}
          {calcResult && (
            <p style={{ margin: 0 }}>
              Created <strong>{calcResult.draftCount}</strong> draft earnings totalling{' '}
              <strong>{INR.format(calcResult.totalDraftAmount)}</strong> for {calcResult.schemeName},{' '}
              {MONTHS[calcResult.month - 1]} {calcResult.year} (achievement{' '}
              {PCT.format(calcResult.achievementPct)}%).
            </p>
          )}
        </div>
      )}

      {/* ---------- earnings ---------- */}
      <div className="card">
        <h2>Earnings</h2>
        <div className="filters">
          <div className="field">
            <label htmlFor="earn-scheme">Scheme</label>
            <select
              id="earn-scheme"
              value={filterSchemeId}
              onChange={(e) => {
                setFilterSchemeId(e.target.value)
                setPage(0)
              }}
            >
              <option value="">All schemes</option>
              {schemes.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="earn-status">Status</label>
            <select
              id="earn-status"
              value={filterStatus}
              onChange={(e) => {
                setFilterStatus(e.target.value)
                setPage(0)
              }}
            >
              <option value="">All statuses</option>
              <option value="DRAFT">Draft</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="earn-year">Year</label>
            <input
              id="earn-year"
              type="number"
              min={2000}
              max={2100}
              value={filterYear}
              onChange={(e) => {
                setFilterYear(e.target.value)
                setPage(0)
              }}
            />
          </div>
          <div className="field">
            <label htmlFor="earn-month">Month</label>
            <select
              id="earn-month"
              value={filterMonth}
              onChange={(e) => {
                setFilterMonth(e.target.value)
                setPage(0)
              }}
            >
              <option value="">All</option>
              {MONTHS.map((m, i) => (
                <option key={m} value={i + 1}>
                  {m}
                </option>
              ))}
            </select>
          </div>
        </div>

        {earningsError && <p className="error-text">{earningsError}</p>}
        {earningsLoading && !earnings && <p className="muted">Loading earnings...</p>}

        {earnings && (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>Dept</th>
                    <th>Scheme</th>
                    <th>Period</th>
                    <th className="num">Achievement</th>
                    <th className="num">Amount</th>
                    <th>Status</th>
                    <th>Decided by</th>
                    {writable && <th aria-label="Actions" />}
                  </tr>
                </thead>
                <tbody>
                  {earningRows.map((e) => (
                    <tr key={e.id}>
                      <td>
                        {e.employee.empNo}
                        <br />
                        <span className="muted">{e.employee.fullName}</span>
                      </td>
                      <td>{e.employee.department.code}</td>
                      <td>{e.schemeName}</td>
                      <td>
                        {MONTHS[e.month - 1]} {e.year}
                      </td>
                      <td className="num">{PCT.format(e.achievementPct)}%</td>
                      <td className="num">{INR.format(e.amount)}</td>
                      <td>
                        <StatusBadge status={e.status} />
                      </td>
                      <td>{e.decidedBy ?? '—'}</td>
                      {writable && (
                        <td>
                          {e.status === 'DRAFT' ? (
                            <>
                              <button className="btn btn-sm btn-primary" onClick={() => handleDecide(e, 'approve')}>
                                Approve
                              </button>{' '}
                              <button className="btn btn-sm btn-danger" onClick={() => handleDecide(e, 'reject')}>
                                Reject
                              </button>
                            </>
                          ) : (
                            <span className="muted">—</span>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                  {earningRows.length === 0 && (
                    <tr>
                      <td colSpan={writable ? 9 : 8} className="muted" style={{ textAlign: 'center' }}>
                        {filtersActive ? 'No earnings match the current filters' : 'No incentive earnings yet'}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            {earnings.totalPages > 0 && (
              <div className="pager">
                <button
                  className="btn btn-sm"
                  disabled={earningsLoading || page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  Prev
                </button>
                <span>
                  Page {page + 1} of {earnings.totalPages} ({earnings.totalElements} total)
                </span>
                <button
                  className="btn btn-sm"
                  disabled={earningsLoading || page + 1 >= earnings.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* ---------- scheme modal ---------- */}
      {schemeModalOpen && (
        <div className="overlay">
          <div className="modal">
            <div className="modal-head">
              <h2>{editingScheme ? `Edit ${editingScheme.name}` : 'Add incentive scheme'}</h2>
              <button className="btn btn-ghost" onClick={() => setSchemeModalOpen(false)}>
                Close
              </button>
            </div>
            <form onSubmit={handleSchemeSubmit}>
              <div className="form-grid">
                <div className="field">
                  <label htmlFor="scheme-name">Name</label>
                  <input
                    id="scheme-name"
                    value={schemeForm.name}
                    onChange={(e) => setSchemeForm({ ...schemeForm, name: e.target.value })}
                    required
                  />
                </div>
                <div className="field">
                  <label htmlFor="scheme-description">Description</label>
                  <input
                    id="scheme-description"
                    value={schemeForm.description}
                    onChange={(e) => setSchemeForm({ ...schemeForm, description: e.target.value })}
                  />
                </div>
                <div className="field">
                  <label htmlFor="scheme-threshold">Threshold %</label>
                  <input
                    id="scheme-threshold"
                    type="number"
                    min={0}
                    step={0.01}
                    value={schemeForm.thresholdPct}
                    onChange={(e) => setSchemeForm({ ...schemeForm, thresholdPct: e.target.value })}
                    required
                  />
                </div>
                <div className="field">
                  <label htmlFor="scheme-ceiling">Ceiling %</label>
                  <input
                    id="scheme-ceiling"
                    type="number"
                    min={0}
                    step={0.01}
                    value={schemeForm.ceilingPct}
                    onChange={(e) => setSchemeForm({ ...schemeForm, ceilingPct: e.target.value })}
                    required
                  />
                </div>
                <div className="field">
                  <label htmlFor="scheme-rate">Rate per point (INR)</label>
                  <input
                    id="scheme-rate"
                    type="number"
                    min={0.01}
                    step={0.01}
                    value={schemeForm.ratePerPoint}
                    onChange={(e) => setSchemeForm({ ...schemeForm, ratePerPoint: e.target.value })}
                    required
                  />
                </div>
                <div className="field">
                  <label htmlFor="scheme-from">Effective from</label>
                  <input
                    id="scheme-from"
                    type="date"
                    value={schemeForm.effectiveFrom}
                    onChange={(e) => setSchemeForm({ ...schemeForm, effectiveFrom: e.target.value })}
                    required
                  />
                </div>
                <div className="field">
                  <label htmlFor="scheme-to">Effective to</label>
                  <input
                    id="scheme-to"
                    type="date"
                    value={schemeForm.effectiveTo}
                    onChange={(e) => setSchemeForm({ ...schemeForm, effectiveTo: e.target.value })}
                  />
                </div>
                <div className="field" style={{ display: 'flex', alignItems: 'end', gap: 8 }}>
                  <input
                    id="scheme-active"
                    type="checkbox"
                    style={{ width: 'auto' }}
                    checked={schemeForm.active}
                    onChange={(e) => setSchemeForm({ ...schemeForm, active: e.target.checked })}
                  />
                  <label htmlFor="scheme-active" style={{ margin: 0 }}>
                    Active
                  </label>
                </div>
              </div>

              <h2 style={{ marginTop: 8 }}>Grade rates</h2>
              <p className="muted">Factor multiplies the per-point rate; employees without a listed grade get 1.00.</p>
              <div className="form-grid">
                {schemeForm.gradeRates.map((rate, index) => (
                  <div key={index} style={{ display: 'flex', gap: 8, alignItems: 'end' }}>
                    <div className="field" style={{ flex: 1 }}>
                      <label htmlFor={`rate-grade-${index}`}>Grade</label>
                      <select
                        id={`rate-grade-${index}`}
                        value={rate.grade}
                        onChange={(e) => setRate(index, { grade: e.target.value })}
                      >
                        <option value="">—</option>
                        {GRADES.map((g) => (
                          <option key={g} value={g}>
                            {g}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="field" style={{ flex: 1 }}>
                      <label htmlFor={`rate-factor-${index}`}>Factor</label>
                      <input
                        id={`rate-factor-${index}`}
                        type="number"
                        min={0.1}
                        step={0.05}
                        value={rate.factor}
                        onChange={(e) => setRate(index, { factor: e.target.value })}
                      />
                    </div>
                    <button
                      type="button"
                      className="btn btn-sm btn-danger"
                      onClick={() =>
                        setSchemeForm((f) => ({
                          ...f,
                          gradeRates: f.gradeRates.filter((_, i) => i !== index),
                        }))
                      }
                    >
                      Remove
                    </button>
                  </div>
                ))}
              </div>
              <button
                type="button"
                className="btn btn-sm"
                onClick={() => setSchemeForm((f) => ({ ...f, gradeRates: [...f.gradeRates, { grade: '', factor: '1.00' }] }))}
              >
                Add grade rate
              </button>

              <div style={{ marginTop: 12 }}>
                {schemeFormError && <p className="error-text">{schemeFormError}</p>}
                <div style={{ display: 'flex', gap: 8 }}>
                  <button type="submit" className="btn btn-primary" disabled={savingScheme}>
                    {savingScheme ? 'Saving...' : editingScheme ? 'Save changes' : 'Create scheme'}
                  </button>
                  <button type="button" className="btn" onClick={() => setSchemeModalOpen(false)}>
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
