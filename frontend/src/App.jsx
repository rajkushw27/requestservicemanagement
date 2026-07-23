import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './AuthContext.jsx'
import Login from './pages/Login.jsx'
import Layout from './pages/Layout.jsx'
import RequestsBoard from './pages/RequestsBoard.jsx'
import RequestDetail from './pages/RequestDetail.jsx'
import NewRequest from './pages/NewRequest.jsx'
import Policies from './pages/Policies.jsx'

function RequireAuth({ children }) {
  const { session } = useAuth()
  if (!session) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/inbox" replace />} />
        <Route path="inbox" element={<RequestsBoard scope="inbox" />} />
        <Route path="submitted" element={<RequestsBoard scope="submitted" />} />
        <Route path="all" element={<RequestsBoard scope="all" />} />
        <Route path="new" element={<NewRequest />} />
        <Route path="policies" element={<Policies />} />
        <Route path="requests/:id" element={<RequestDetail />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
