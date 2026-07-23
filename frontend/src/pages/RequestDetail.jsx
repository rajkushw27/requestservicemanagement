import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'
import { formatMoney, formatDate, STATUS_LABEL } from '../format.js'

function DiffTable({ before, after }) {
  const keys = Array.from(new Set([...Object.keys(before || {}), ...Object.keys(after || {})]))
  if (keys.length === 0) return <p className="muted">No structured payload on this request.</p>
  return (
    <table className="diff-table">
      <thead>
        <tr>
          <th>Field</th>
          <th>Before</th>
          <th>After</th>
        </tr>
      </thead>
      <tbody>
        {keys.map((k) => {
          const b = before?.[k]
          const a = after?.[k]
          const changed = JSON.stringify(b) !== JSON.stringify(a)
          return (
            <tr key={k} className={changed ? 'diff-changed' : ''}>
              <td>{k}</td>
              <td>{b === undefined ? '—' : String(b)}</td>
              <td>{a === undefined ? '—' : String(a)}</td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}

export default function RequestDetail() {
  const { id } = useParams()
  const { session, logout } = useAuth()
  const navigate = useNavigate()
  const [req, setReq] = useState(null)
  const [audit, setAudit] = useState(null)
  const [error, setError] = useState('')
  const [comments, setComments] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(() => {
    setError('')
    Promise.all([api.requestDetail(session.token, id), api.auditFor(session.token, id)])
      .then(([detail, auditTrail]) => {
        setReq(detail)
        setAudit(auditTrail)
      })
      .catch((err) => {
        if (err.status === 401) {
          logout()
          navigate('/login', { replace: true })
          return
        }
        setError(err.message)
      })
  }, [id, session, logout, navigate])

  useEffect(() => {
    load()
  }, [load])

  async function act(decision) {
    setError('')
    if (decision !== 'APPROVE' && !comments.trim()) {
      setError('A comment is required when you reject or send back a request.')
      return
    }
    setBusy(true)
    try {
      const updated = await api.decide(session.token, id, {
        decision,
        comments: comments.trim() || null,
        expectedVersion: req.version,
      })
      setReq(updated)
      setComments('')
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function withdraw() {
    setError('')
    setBusy(true)
    try {
      const updated = await api.recall(session.token, id)
      setReq(updated)
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (!req && !error) return <div className="page"><p className="muted">Loading…</p></div>
  if (error && !req) return <div className="page"><p className="form-error">{error}</p></div>

  const me = session.employee.id
  const isMaker = req.makerId === me
  const canAct = req.status === 'PENDING' && req.currentlyActionableBy.includes(me)
  const canWithdraw = req.status === 'PENDING' && isMaker

  return (
    <div className="page">
      <button className="back-link" onClick={() => navigate(-1)}>← Back</button>

      <div className="detail-header">
        <div>
          <span className={`status-pill status-${req.status.toLowerCase()}`}>{STATUS_LABEL[req.status]}</span>
          <h1>{req.summary}</h1>
          <p className="muted">
            {req.requestId} · {req.entityType} · {req.entityId} · raised by {req.makerId} on {formatDate(req.createdAt)}
          </p>
        </div>
        <div className="detail-amount">{formatMoney(req.amount)}</div>
      </div>

      {isMaker && canAct === false && req.status === 'PENDING' && (
        <p className="notice">You raised this request, so segregation of duties keeps you from signing it yourself.</p>
      )}

      {error && <p className="form-error">{error}</p>}

      <section className="card">
        <h2>What changes</h2>
        <DiffTable before={req.before} after={req.after} />
      </section>

      <section className="card">
        <h2>Approver chain — {req.mode?.toLowerCase()}, {req.requiredApprovals} signature{req.requiredApprovals > 1 ? 's' : ''} required</h2>
        <ul className="chain-list">
          {req.approverChain.map((c) => (
            <li key={c.employeeId} className={`chain-item chain-${(c.decision || 'waiting').toLowerCase()}`}>
              <span className="chain-id">{c.employeeId}</span>
              <span className="chain-status">
                {c.decision ? c.decision.replace('_', ' ').toLowerCase() : 'waiting'}
                {c.decidedAt ? ` · ${formatDate(c.decidedAt)}` : ''}
              </span>
              {c.comments && <span className="chain-comment">"{c.comments}"</span>}
            </li>
          ))}
        </ul>
      </section>

      {canAct && (
        <section className="card">
          <h2>Your decision</h2>
          <textarea
            placeholder="Comments (required to reject)"
            value={comments}
            onChange={(e) => setComments(e.target.value)}
            rows={3}
          />
          <div className="action-row">
            <button className="btn btn-primary" disabled={busy} onClick={() => act('APPROVE')}>
              Approve
            </button>
            <button className="btn btn-danger" disabled={busy} onClick={() => act('REJECT')}>
              Reject
            </button>
          </div>
        </section>
      )}

      {canWithdraw && (
        <section className="card">
          <button className="btn btn-ghost" disabled={busy} onClick={withdraw}>
            Withdraw request
          </button>
        </section>
      )}

      <section className="card">
        <h2>Audit trail</h2>
        <ul className="audit-list">
          {(audit || []).map((e) => (
            <li key={e.id}>
              <span className="audit-actor">{e.actor}</span>
              <span className="audit-action">{e.action.replace(/_/g, ' ').toLowerCase()}</span>
              <span className="audit-time">{formatDate(e.createdAt)}</span>
              {e.detail && <div className="audit-detail">{e.detail}</div>}
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
