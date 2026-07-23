import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'
import { formatMoney, formatDate, timeUntil, STATUS_LABEL } from '../format.js'

const SCOPE_COPY = {
  inbox: {
    title: 'Waiting on you',
    empty: "Nothing needs your signature right now. Try 'All requests' to see everything in flight.",
  },
  submitted: {
    title: 'Requests you raised',
    empty: "You haven't submitted anything yet. Use '+ New request' to raise one.",
  },
  all: {
    title: 'All requests',
    empty: 'No requests yet.',
  },
}

export default function RequestsBoard({ scope }) {
  const { session, logout } = useAuth()
  const navigate = useNavigate()
  const [items, setItems] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    setItems(null)
    setError('')

    const params = {}
    if (scope === 'inbox') params.inboxFor = session.employee.id
    if (scope === 'submitted') params.submittedBy = session.employee.id

    api
      .requests(session.token, params)
      .then((data) => {
        if (!cancelled) setItems(data)
      })
      .catch((err) => {
        if (cancelled) return
        if (err.status === 401) {
          logout()
          navigate('/login', { replace: true })
          return
        }
        setError(err.message)
      })

    return () => {
      cancelled = true
    }
  }, [scope, session, logout, navigate])

  const copy = SCOPE_COPY[scope]

  return (
    <div className="page">
      <div className="page-header">
        <h1>{copy.title}</h1>
        {items && <span className="count-badge">{items.length}</span>}
      </div>

      {error && <p className="form-error">{error}</p>}

      {items === null && !error && <p className="muted">Loading…</p>}

      {items && items.length === 0 && <p className="muted">{copy.empty}</p>}

      {items && items.length > 0 && (
        <div className="request-list">
          {items.map((r) => (
            <button key={r.id} className="request-card" onClick={() => navigate(`/requests/${r.id}`)}>
              <div className="request-card-top">
                <span className={`status-pill status-${r.status.toLowerCase()}`}>{STATUS_LABEL[r.status]}</span>
                <span className="request-id">{r.requestId}</span>
                {scope !== 'inbox' && r.status === 'PENDING' && (
                  <span className="due-chip">{timeUntil(r.deadlineAt)}</span>
                )}
              </div>
              <div className="request-summary">{r.summary}</div>
              <div className="request-meta">
                <span>{r.entityType} · {r.entityId}</span>
                <span>{formatMoney(r.amount)}</span>
                <span>{r.requiredApprovals} signature{r.requiredApprovals > 1 ? 's' : ''} · {r.mode?.toLowerCase()}</span>
                <span>{formatDate(r.createdAt)}</span>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
