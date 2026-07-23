import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext.jsx'

const QUICK_PICKS = [
  { id: 'emp-15', name: 'Manish Tiwari', role: 'Maker — Limits Analyst', note: 'raised REQ-1001' },
  { id: 'emp-09', name: 'Arjun Pillai', role: 'Checker — Manager, Limits', note: 'signs REQ-1001 first' },
  { id: 'emp-04', name: 'Ananya Rao', role: 'Checker — Director, Payment Ops', note: 'signs REQ-1001 second' },
  { id: 'emp-16', name: 'Pooja Shetty', role: 'Maker — Treasury Analyst', note: 'raised the ₹84L transfer' },
  { id: 'emp-01', name: 'Aarav Mehta', role: 'CRO — top of every chain', note: 'can approve anything' },
]

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await login(username.trim(), password)
      navigate('/inbox', { replace: true })
    } catch (err) {
      setError(err.message || 'Sign in failed')
    } finally {
      setBusy(false)
    }
  }

  function quickPick(id) {
    setUsername(id)
    setPassword('test123')
  }

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <div className="brand">
          <span className="brand-mark">✓</span>
          <div>
            <h1>Counterfoil</h1>
            <p>Maker-checker approvals, demo instance</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          <label>
            User ID
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="e.g. emp-15"
              autoComplete="username"
              autoFocus
            />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="test123"
              autoComplete="current-password"
            />
          </label>
          {error && <p className="form-error">{error}</p>}
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="quick-picks">
          <p className="quick-picks-label">Demo accounts — password is <code>test123</code> for all 20</p>
          <ul>
            {QUICK_PICKS.map((p) => (
              <li key={p.id}>
                <button type="button" onClick={() => quickPick(p.id)}>
                  <span className="qp-id">{p.id}</span>
                  <span className="qp-name">{p.name}</span>
                  <span className="qp-role">{p.role}</span>
                  <span className="qp-note">{p.note}</span>
                </button>
              </li>
            ))}
          </ul>
          <p className="quick-picks-hint">Pick one to fill the form, then hit Sign in. Full org chart is in the README.</p>
        </div>
      </div>
    </div>
  )
}
