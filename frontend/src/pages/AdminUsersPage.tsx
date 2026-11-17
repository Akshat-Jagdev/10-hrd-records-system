import { useEffect, useState, type FormEvent } from 'react'
import { apiError, createUser, deleteUser, listUsers, updateUser } from '../api'
import StatusBadge from '../components/StatusBadge'
import type { Role, UserSummary } from '../types'

const ROLES: Role[] = ['ADMIN', 'HR_MANAGER', 'VIEWER', 'AUDITOR']

interface CreateUserForm {
  username: string
  password: string
  fullName: string
  role: Role
}

interface EditUserForm {
  fullName: string
  role: Role
  enabled: boolean
}

const EMPTY_CREATE: CreateUserForm = { username: '', password: '', fullName: '', role: 'VIEWER' }

export default function AdminUsersPage() {
  const [users, setUsers] = useState<UserSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadFlag, setReloadFlag] = useState(0)

  const [modal, setModal] = useState<'create' | 'edit' | null>(null)
  const [editing, setEditing] = useState<UserSummary | null>(null)
  const [createForm, setCreateForm] = useState<CreateUserForm>(EMPTY_CREATE)
  const [editForm, setEditForm] = useState<EditUserForm>({ fullName: '', role: 'VIEWER', enabled: true })
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    listUsers()
      .then((res) => {
        if (cancelled) return
        setUsers(res)
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
    setCreateForm(EMPTY_CREATE)
    setFormError('')
    setModal('create')
  }

  function openEdit(user: UserSummary) {
    setEditing(user)
    setEditForm({ fullName: user.fullName, role: user.role, enabled: user.enabled })
    setFormError('')
    setModal('edit')
  }

  function closeModal() {
    setModal(null)
    setFormError('')
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      await createUser(createForm)
      setModal(null)
      setReloadFlag((n) => n + 1)
    } catch (e) {
      setFormError(apiError(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleEdit(event: FormEvent) {
    event.preventDefault()
    if (!editing) return
    setSaving(true)
    setFormError('')
    try {
      await updateUser(editing.id, editForm)
      setModal(null)
      setReloadFlag((n) => n + 1)
    } catch (e) {
      setFormError(apiError(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(user: UserSummary) {
    if (!window.confirm(`Delete user ${user.username} (${user.fullName})?`)) return
    try {
      await deleteUser(user.id)
      setReloadFlag((n) => n + 1)
    } catch (e) {
      setError(apiError(e))
    }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Users</h1>
          <p className="muted">Administration accounts — ADMIN access only</p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>
          Add user
        </button>
      </div>

      {error && <p className="error-text">{error}</p>}
      {loading && <p className="muted">Loading users...</p>}

      {!loading && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Username</th>
                <th>Full name</th>
                <th>Role</th>
                <th>Status</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.username}</td>
                  <td>{u.fullName}</td>
                  <td>{u.role}</td>
                  <td>
                    <StatusBadge status={u.enabled} />
                  </td>
                  <td>
                    <button className="btn btn-sm" onClick={() => openEdit(u)}>
                      Edit
                    </button>{' '}
                    <button className="btn btn-sm btn-danger" onClick={() => handleDelete(u)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={5} className="muted" style={{ textAlign: 'center' }}>
                    No users found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {modal === 'create' && (
        <div className="overlay">
          <div className="modal">
            <div className="modal-head">
              <h2>Add user</h2>
              <button className="btn btn-ghost" onClick={closeModal}>
                Close
              </button>
            </div>
            <form className="form-grid" onSubmit={handleCreate}>
              <div className="field">
                <label htmlFor="user-username">Username</label>
                <input
                  id="user-username"
                  value={createForm.username}
                  onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="user-fullName">Full name</label>
                <input
                  id="user-fullName"
                  value={createForm.fullName}
                  onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="user-password">Password</label>
                <input
                  id="user-password"
                  type="password"
                  minLength={8}
                  value={createForm.password}
                  onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="user-role">Role</label>
                <select
                  id="user-role"
                  value={createForm.role}
                  onChange={(e) => setCreateForm({ ...createForm, role: e.target.value as Role })}
                >
                  {ROLES.map((r) => (
                    <option key={r} value={r}>
                      {r}
                    </option>
                  ))}
                </select>
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                {formError && <p className="error-text">{formError}</p>}
                <div style={{ display: 'flex', gap: 8 }}>
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? 'Saving...' : 'Create user'}
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

      {modal === 'edit' && editing && (
        <div className="overlay">
          <div className="modal">
            <div className="modal-head">
              <h2>Edit {editing.username}</h2>
              <button className="btn btn-ghost" onClick={closeModal}>
                Close
              </button>
            </div>
            <form className="form-grid" onSubmit={handleEdit}>
              <div className="field">
                <label htmlFor="edit-fullName">Full name</label>
                <input
                  id="edit-fullName"
                  value={editForm.fullName}
                  onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="edit-role">Role</label>
                <select
                  id="edit-role"
                  value={editForm.role}
                  onChange={(e) => setEditForm({ ...editForm, role: e.target.value as Role })}
                >
                  {ROLES.map((r) => (
                    <option key={r} value={r}>
                      {r}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field" style={{ display: 'flex', alignItems: 'end', gap: 8 }}>
                <input
                  id="edit-enabled"
                  type="checkbox"
                  style={{ width: 'auto' }}
                  checked={editForm.enabled}
                  onChange={(e) => setEditForm({ ...editForm, enabled: e.target.checked })}
                />
                <label htmlFor="edit-enabled" style={{ margin: 0 }}>
                  Enabled
                </label>
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                {formError && <p className="error-text">{formError}</p>}
                <div style={{ display: 'flex', gap: 8 }}>
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? 'Saving...' : 'Save changes'}
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
