import { useEffect, useState, type FormEvent } from 'react'
import {
  apiError,
  createEmployee,
  deleteEmployee,
  listDepartments,
  searchEmployees,
  updateEmployee,
} from '../api'
import { canWrite, useAuth } from '../auth'
import StatusBadge from '../components/StatusBadge'
import type { DepartmentSummary, EmployeeSummary, PageResponse } from '../types'

const PAGE_SIZE = 20
const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })

interface EmployeeFormState {
  empNo: string
  fullName: string
  grade: string
  designation: string
  departmentId: string
  doj: string
  status: string
  basicPay: string
}

const EMPTY_FORM: EmployeeFormState = {
  empNo: '',
  fullName: '',
  grade: '',
  designation: '',
  departmentId: '',
  doj: '',
  status: 'ACTIVE',
  basicPay: '',
}

function formFromEmployee(employee: EmployeeSummary): EmployeeFormState {
  return {
    empNo: employee.empNo,
    fullName: employee.fullName,
    grade: employee.grade,
    designation: employee.designation,
    departmentId: String(employee.department.id),
    doj: employee.doj.slice(0, 10),
    status: employee.status,
    basicPay: String(employee.basicPay),
  }
}

export default function EmployeesPage() {
  const { user } = useAuth()
  const writable = canWrite(user)

  // Filter inputs; the two text inputs are debounced before hitting the API.
  const [empNoInput, setEmpNoInput] = useState('')
  const [nameInput, setNameInput] = useState('')
  const [departmentId, setDepartmentId] = useState('')
  const [status, setStatus] = useState('')

  // Applied (debounced) filters + paging.
  const [empNo, setEmpNo] = useState('')
  const [name, setName] = useState('')
  const [page, setPage] = useState(0)

  const [data, setData] = useState<PageResponse<EmployeeSummary> | null>(null)
  const [departments, setDepartments] = useState<DepartmentSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadFlag, setReloadFlag] = useState(0)

  // Create/edit modal state.
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<EmployeeSummary | null>(null)
  const [form, setForm] = useState<EmployeeFormState>(EMPTY_FORM)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  // Debounce the text filters ~300ms; select filters apply immediately via handlers.
  useEffect(() => {
    const timer = setTimeout(() => {
      setEmpNo(empNoInput.trim())
      setName(nameInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [empNoInput, nameInput])

  // Department options for the filter and the modal select.
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
    searchEmployees({
      empNo: empNo || undefined,
      name: name || undefined,
      departmentId: departmentId ? Number(departmentId) : undefined,
      status: status || undefined,
      page,
      size: PAGE_SIZE,
    })
      .then((res) => {
        if (cancelled) return
        setData(res)
        setLoading(false)
        // Clamp the page when it shrank out from under us (e.g. after a delete).
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
  }, [empNo, name, departmentId, status, page, reloadFlag])

  function openCreate() {
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormError('')
    setModalOpen(true)
  }

  function openEdit(employee: EmployeeSummary) {
    setEditing(employee)
    setForm(formFromEmployee(employee))
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
      empNo: form.empNo.trim(),
      fullName: form.fullName.trim(),
      grade: form.grade.trim(),
      designation: form.designation.trim(),
      departmentId: Number(form.departmentId),
      doj: form.doj,
      status: form.status,
      basicPay: Number(form.basicPay),
    }
    try {
      if (editing) {
        await updateEmployee(editing.id, body)
      } else {
        await createEmployee(body)
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

  async function handleDelete(employee: EmployeeSummary) {
    if (!window.confirm(`Delete employee ${employee.empNo} (${employee.fullName})?`)) return
    try {
      await deleteEmployee(employee.id)
      setReloadFlag((n) => n + 1)
    } catch (e) {
      setError(apiError(e))
    }
  }

  const filtersActive = empNo !== '' || name !== '' || departmentId !== '' || status !== ''
  const rows = data?.content ?? []

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Employees</h1>
          <p className="muted">Employee master records</p>
        </div>
        {writable && (
          <button className="btn btn-primary" onClick={openCreate}>
            Add employee
          </button>
        )}
      </div>

      <div className="filters">
        <div className="field">
          <label htmlFor="filter-emp-no">Employee no</label>
          <input
            id="filter-emp-no"
            value={empNoInput}
            onChange={(e) => setEmpNoInput(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="filter-name">Name</label>
          <input id="filter-name" value={nameInput} onChange={(e) => setNameInput(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="filter-department">Department</label>
          <select
            id="filter-department"
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
          <label htmlFor="filter-status">Status</label>
          <select
            id="filter-status"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value)
              setPage(0)
            }}
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="RETIRED">Retired</option>
            <option value="TRANSFERRED">Transferred</option>
          </select>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}
      {loading && !data && <p className="muted">Loading employees...</p>}

      {data && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Employee no</th>
                  <th>Name</th>
                  <th>Grade</th>
                  <th>Designation</th>
                  <th>Dept</th>
                  <th>Date of joining</th>
                  <th>Status</th>
                  <th className="num">Basic pay</th>
                  {writable && <th aria-label="Actions" />}
                </tr>
              </thead>
              <tbody>
                {rows.map((e) => (
                  <tr key={e.id}>
                    <td>{e.empNo}</td>
                    <td>{e.fullName}</td>
                    <td>{e.grade}</td>
                    <td>{e.designation}</td>
                    <td>{e.department.code}</td>
                    <td>{new Date(e.doj).toLocaleDateString('en-IN')}</td>
                    <td>
                      <StatusBadge status={e.status} />
                    </td>
                    <td className="num">{inr.format(e.basicPay)}</td>
                    {writable && (
                      <td>
                        <button className="btn btn-sm" onClick={() => openEdit(e)}>
                          Edit
                        </button>{' '}
                        <button className="btn btn-sm btn-danger" onClick={() => handleDelete(e)}>
                          Delete
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
                {rows.length === 0 && (
                  <tr>
                    <td colSpan={writable ? 9 : 8} className="muted" style={{ textAlign: 'center' }}>
                      {filtersActive ? 'No employees match the current filters' : 'No employees yet'}
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
              <h2>{editing ? `Edit ${editing.empNo}` : 'Add employee'}</h2>
              <button className="btn btn-ghost" onClick={closeModal}>
                Close
              </button>
            </div>
            <form className="form-grid" onSubmit={handleSubmit}>
              <div className="field">
                <label htmlFor="emp-empNo">Employee no</label>
                <input
                  id="emp-empNo"
                  value={form.empNo}
                  onChange={(e) => setForm({ ...form, empNo: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="emp-fullName">Full name</label>
                <input
                  id="emp-fullName"
                  value={form.fullName}
                  onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="emp-grade">Grade</label>
                <input
                  id="emp-grade"
                  value={form.grade}
                  onChange={(e) => setForm({ ...form, grade: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="emp-designation">Designation</label>
                <input
                  id="emp-designation"
                  value={form.designation}
                  onChange={(e) => setForm({ ...form, designation: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="emp-department">Department</label>
                <select
                  id="emp-department"
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
                <label htmlFor="emp-doj">Date of joining</label>
                <input
                  id="emp-doj"
                  type="date"
                  value={form.doj}
                  onChange={(e) => setForm({ ...form, doj: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="emp-status">Status</label>
                <select
                  id="emp-status"
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="RETIRED">Retired</option>
                  <option value="TRANSFERRED">Transferred</option>
                </select>
              </div>
              <div className="field">
                <label htmlFor="emp-basicPay">Basic pay</label>
                <input
                  id="emp-basicPay"
                  type="number"
                  min={0}
                  value={form.basicPay}
                  onChange={(e) => setForm({ ...form, basicPay: e.target.value })}
                  required
                />
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                {formError && <p className="error-text">{formError}</p>}
                <div style={{ display: 'flex', gap: 8 }}>
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? 'Saving...' : editing ? 'Save changes' : 'Create employee'}
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
