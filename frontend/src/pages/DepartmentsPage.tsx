import { useEffect, useState, type FormEvent } from 'react'
import { apiError, createDepartment, deleteDepartment, listDepartments, updateDepartment } from '../api'
import { canWrite, useAuth } from '../auth'
import type { DepartmentSummary } from '../types'

interface DepartmentFormState {
  code: string
  name: string
  type: string
}

const EMPTY_FORM: DepartmentFormState = { code: '', name: '', type: 'PRODUCTION' }

const DEPARTMENT_TYPES = ['PRODUCTION', 'SUPPORT']

export default function DepartmentsPage() {
  const { user } = useAuth()
  const writable = canWrite(user)

  const [departments, setDepartments] = useState<DepartmentSummary[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadFlag, setReloadFlag] = useState(0)

  // Create/edit modal state.
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<DepartmentSummary | null>(null)
  const [form, setForm] = useState<DepartmentFormState>(EMPTY_FORM)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    listDepartments()
      .then((res) => {
        if (cancelled) return
        setDepartments([...res].sort((a, b) => a.code.localeCompare(b.code)))
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
  }, [reloadFlag])

  function openCreate() {
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormError('')
    setModalOpen(true)
  }

  function openEdit(department: DepartmentSummary) {
    setEditing(department)
    setForm({ code: department.code, name: department.name, type: department.type })
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
    const body = { code: form.code.trim(), name: form.name.trim(), type: form.type }
    try {
      if (editing) {
        await updateDepartment(editing.id, body)
      } else {
        await createDepartment(body)
      }
      setModalOpen(false)
      setReloadFlag((n) => n + 1)
    } catch (e) {
      setFormError(apiError(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(department: DepartmentSummary) {
    if (!window.confirm(`Delete department ${department.code} (${department.name})?`)) return
    try {
      await deleteDepartment(department.id)
      setReloadFlag((n) => n + 1)
    } catch (e) {
      // e.g. a 409 because the department still has employees or production records.
      setError(apiError(e))
    }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Departments</h1>
          <p className="muted">Production and support departments</p>
        </div>
        {writable && (
          <button className="btn btn-primary" onClick={openCreate}>
            Add department
          </button>
        )}
      </div>

      {error && <p className="error-text">{error}</p>}
      {loading && !departments && <p className="muted">Loading departments...</p>}

      {departments && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Type</th>
                {writable && <th aria-label="Actions" />}
              </tr>
            </thead>
            <tbody>
              {departments.map((d) => (
                <tr key={d.id}>
                  <td>{d.code}</td>
                  <td>{d.name}</td>
                  <td>
                    <span className="badge">
                      <span className="badge-dot" aria-hidden="true" />
                      {d.type}
                    </span>
                  </td>
                  {writable && (
                    <td>
                      <button className="btn btn-sm" onClick={() => openEdit(d)}>
                        Edit
                      </button>{' '}
                      <button className="btn btn-sm btn-danger" onClick={() => handleDelete(d)}>
                        Delete
                      </button>
                    </td>
                  )}
                </tr>
              ))}
              {departments.length === 0 && (
                <tr>
                  <td colSpan={writable ? 4 : 3} className="muted" style={{ textAlign: 'center' }}>
                    No departments yet
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {modalOpen && (
        <div className="overlay">
          <div className="modal">
            <div className="modal-head">
              <h2>{editing ? `Edit ${editing.code}` : 'Add department'}</h2>
              <button className="btn btn-ghost" onClick={closeModal}>
                Close
              </button>
            </div>
            <form className="form-grid" onSubmit={handleSubmit}>
              <div className="field">
                <label htmlFor="dept-code">Code</label>
                <input
                  id="dept-code"
                  value={form.code}
                  onChange={(e) => setForm({ ...form, code: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="dept-name">Name</label>
                <input
                  id="dept-name"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="dept-type">Type</label>
                <select
                  id="dept-type"
                  value={form.type}
                  onChange={(e) => setForm({ ...form, type: e.target.value })}
                >
                  {DEPARTMENT_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                {formError && <p className="error-text">{formError}</p>}
                <div style={{ display: 'flex', gap: 8 }}>
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? 'Saving...' : editing ? 'Save changes' : 'Create department'}
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
