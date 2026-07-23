import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'
import { formatMoney } from '../format.js'

function PolicyCard({ policy, onSave }) {
  const [editing, setEditing] = useState(false)
  const [minApprovals, setMinApprovals] = useState(policy.minApprovals)
  const [slaMinutes, setSlaMinutes] = useState(policy.slaMinutes)
  const [amountThreshold, setAmountThreshold] = useState(policy.amountThreshold ?? '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function save() {
    setBusy(true)
    setError('')
    try {
      await onSave(policy.policyKey, {
        minApprovals: Number(minApprovals),
        slaMinutes: Number(slaMinutes),
        amountThreshold: amountThreshold === '' ? null : Number(amountThreshold),
      })
      setEditing(false)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card policy-card">
      <div className="policy-card-header">
        <h2>{policy.policyKey}</h2>
        <span className="mode-chip">{policy.mode?.toLowerCase()}</span>
      </div>
      <p className="muted">{policy.description}</p>

      {!editing ? (
        <>
          <dl className="policy-facts">
            <div><dt>Signatures</dt><dd>{policy.minApprovals}</dd></div>
            <div><dt>High-value bump above</dt><dd>{policy.amountThreshold ? formatMoney(policy.amountThreshold) : 'n/a'}</dd></div>
            <div><dt>Roles allowed</dt><dd>{policy.allowedApproverRoles}</dd></div>
            <div><dt>SLA</dt><dd>{policy.slaMinutes} min, then {policy.onTimeout?.toLowerCase()}</dd></div>
            <div><dt>Self-recall</dt><dd>{policy.allowSelfRecall ? 'allowed' : 'not allowed'}</dd></div>
          </dl>
          <button className="btn btn-ghost btn-sm" onClick={() => setEditing(true)}>Edit</button>
        </>
      ) : (
        <>
          <div className="form-grid">
            <label>
              Signatures required
              <input type="number" min={1} value={minApprovals} onChange={(e) => setMinApprovals(e.target.value)} />
            </label>
            <label>
              High-value bump above (₹)
              <input type="number" value={amountThreshold} onChange={(e) => setAmountThreshold(e.target.value)} placeholder="none" />
            </label>
            <label>
              SLA (minutes)
              <input type="number" min={1} value={slaMinutes} onChange={(e) => setSlaMinutes(e.target.value)} />
            </label>
          </div>
          {error && <p className="form-error">{error}</p>}
          <div className="action-row">
            <button className="btn btn-primary btn-sm" disabled={busy} onClick={save}>Save</button>
            <button className="btn btn-ghost btn-sm" disabled={busy} onClick={() => setEditing(false)}>Cancel</button>
          </div>
        </>
      )}
    </div>
  )
}

export default function Policies() {
  const { session } = useAuth()
  const [policies, setPolicies] = useState(null)
  const [error, setError] = useState('')

  function load() {
    api.policies(session.token).then(setPolicies).catch((err) => setError(err.message))
  }

  useEffect(load, [session])

  async function handleSave(key, body) {
    const updated = await api.updatePolicy(session.token, key, body)
    setPolicies((prev) => prev.map((p) => (p.policyKey === key ? updated : p)))
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Policies</h1>
      </div>
      <p className="muted">Editable at runtime — no redeploy needed. Changes only apply to requests submitted after the edit; in-flight requests keep the approver chain they were frozen with at submit time.</p>
      {error && <p className="form-error">{error}</p>}
      {!policies && !error && <p className="muted">Loading…</p>}
      {policies && (
        <div className="policy-grid">
          {policies.map((p) => (
            <PolicyCard key={p.policyKey} policy={p} onSave={handleSave} />
          ))}
        </div>
      )}
    </div>
  )
}
