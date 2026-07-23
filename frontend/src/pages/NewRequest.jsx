import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'

function KeyValueEditor({ rows, setRows }) {
  function update(i, field, value) {
    const next = rows.slice()
    next[i] = { ...next[i], [field]: value }
    setRows(next)
  }
  function add() {
    setRows([...rows, { key: '', value: '' }])
  }
  function remove(i) {
    setRows(rows.filter((_, idx) => idx !== i))
  }
  return (
    <div className="kv-editor">
      {rows.map((row, i) => (
        <div className="kv-row" key={i}>
          <input placeholder="field" value={row.key} onChange={(e) => update(i, 'key', e.target.value)} />
          <input placeholder="value" value={row.value} onChange={(e) => update(i, 'value', e.target.value)} />
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => remove(i)}>✕</button>
        </div>
      ))}
      <button type="button" className="btn btn-ghost btn-sm" onClick={add}>+ field</button>
    </div>
  )
}

function rowsToObject(rows) {
  const obj = {}
  for (const { key, value } of rows) {
    if (!key.trim()) continue
    const num = Number(value)
    obj[key.trim()] = value !== '' && !Number.isNaN(num) ? num : value
  }
  return Object.keys(obj).length ? obj : null
}

export default function NewRequest() {
  const { session } = useAuth()
  const navigate = useNavigate()
  const [policies, setPolicies] = useState([])
  const [form, setForm] = useState({
    requestId: `REQ-${Date.now()}`,
    entityType: '',
    entityId: '',
    operation: 'UPDATE',
    summary: '',
    amount: '',
    policyKey: '',
    callbackUrl: '',
  })
  const [beforeRows, setBeforeRows] = useState([{ key: '', value: '' }])
  const [afterRows, setAfterRows] = useState([{ key: '', value: '' }])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api.policies(session.token).then((data) => {
      setPolicies(data)
      if (data.length && !form.policyKey) {
        setForm((f) => ({ ...f, policyKey: data[0].policyKey }))
      }
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session])

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    if (!form.entityType.trim() || !form.entityId.trim() || !form.summary.trim() || !form.policyKey) {
      setError('Entity type, entity ID, summary and policy are required.')
      return
    }
    setBusy(true)
    try {
      const body = {
        requestId: form.requestId,
        tenantId: 'default',
        entityType: form.entityType.trim(),
        entityId: form.entityId.trim(),
        operation: form.operation,
        summary: form.summary.trim(),
        before: rowsToObject(beforeRows),
        after: rowsToObject(afterRows),
        amount: form.amount ? Number(form.amount) : null,
        policyKey: form.policyKey,
        callbackUrl: form.callbackUrl.trim() || null,
      }
      const created = await api.submit(session.token, body)
      navigate(`/requests/${created.id}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page page-narrow">
      <div className="page-header">
        <h1>New approval request</h1>
      </div>

      <form className="card form" onSubmit={handleSubmit}>
        <label>
          Summary
          <input value={form.summary} onChange={(e) => set('summary', e.target.value)} placeholder="What are you changing, in plain English" />
        </label>

        <div className="form-grid">
          <label>
            Entity type
            <input value={form.entityType} onChange={(e) => set('entityType', e.target.value)} placeholder="CUSTOMER_LIMIT" />
          </label>
          <label>
            Entity ID
            <input value={form.entityId} onChange={(e) => set('entityId', e.target.value)} placeholder="CUST-88213" />
          </label>
          <label>
            Operation
            <select value={form.operation} onChange={(e) => set('operation', e.target.value)}>
              <option value="CREATE">Create</option>
              <option value="UPDATE">Update</option>
              <option value="DELETE">Delete</option>
            </select>
          </label>
          <label>
            Amount (₹, optional)
            <input type="number" value={form.amount} onChange={(e) => set('amount', e.target.value)} placeholder="750000" />
          </label>
        </div>

        <label>
          Policy
          <select value={form.policyKey} onChange={(e) => set('policyKey', e.target.value)}>
            {policies.map((p) => (
              <option key={p.policyKey} value={p.policyKey}>
                {p.policyKey} — {p.description}
              </option>
            ))}
          </select>
        </label>

        <div className="form-grid-2">
          <div>
            <p className="field-label">Before</p>
            <KeyValueEditor rows={beforeRows} setRows={setBeforeRows} />
          </div>
          <div>
            <p className="field-label">After</p>
            <KeyValueEditor rows={afterRows} setRows={setAfterRows} />
          </div>
        </div>

        <label>
          Callback URL (optional)
          <input value={form.callbackUrl} onChange={(e) => set('callbackUrl', e.target.value)} placeholder="https://your-system.example/hooks/approvals" />
        </label>

        {error && <p className="form-error">{error}</p>}

        <div className="action-row">
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Submitting…' : 'Submit for approval'}
          </button>
        </div>
      </form>
    </div>
  )
}
