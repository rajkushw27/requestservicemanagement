const BASE = '/api/v1'

class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

async function request(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (res.status === 204) return null

  const data = await res.json().catch(() => null)
  if (!res.ok) {
    const message = data?.message || `Request failed (${res.status})`
    throw new ApiError(res.status, message)
  }
  return data
}

export const api = {
  login: (username, password) => request('/auth/login', { method: 'POST', body: { username, password } }),
  people: (token) => request('/people', { token }),
  policies: (token) => request('/policies', { token }),
  updatePolicy: (token, key, body) => request(`/policies/${key}`, { method: 'PUT', body, token }),
  requests: (token, params = {}) => {
    const qs = new URLSearchParams(params).toString()
    return request(`/approval-requests${qs ? `?${qs}` : ''}`, { token })
  },
  requestDetail: (token, id) => request(`/approval-requests/${id}`, { token }),
  auditFor: (token, id) => request(`/approval-requests/${id}/audit`, { token }),
  submit: (token, body) => request('/approval-requests', { method: 'POST', body, token }),
  decide: (token, id, body) => request(`/approval-requests/${id}/decisions`, { method: 'POST', body, token }),
  recall: (token, id) => request(`/approval-requests/${id}/recall`, { method: 'POST', token }),
}

export { ApiError }
