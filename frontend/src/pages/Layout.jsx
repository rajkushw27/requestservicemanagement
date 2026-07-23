import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext.jsx'

export default function Layout() {
  const { session, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  const emp = session.employee

  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar-brand">
          <span className="brand-mark">✓</span>
          <span>Counterfoil</span>
        </div>
        <nav className="topbar-nav">
          <NavLink to="/inbox" className={({ isActive }) => (isActive ? 'active' : '')}>
            Waiting on me
          </NavLink>
          <NavLink to="/submitted" className={({ isActive }) => (isActive ? 'active' : '')}>
            I submitted
          </NavLink>
          <NavLink to="/all" className={({ isActive }) => (isActive ? 'active' : '')}>
            All requests
          </NavLink>
          <NavLink to="/policies" className={({ isActive }) => (isActive ? 'active' : '')}>
            Policies
          </NavLink>
        </nav>
        <div className="topbar-actions">
          <NavLink to="/new" className="btn btn-primary btn-sm">
            + New request
          </NavLink>
          <div className="whoami">
            <div className="whoami-name">{emp.name}</div>
            <div className="whoami-title">{emp.title}</div>
          </div>
          <button type="button" className="btn btn-ghost btn-sm" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
