const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })
const dateTime = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' })

export function formatMoney(amount) {
  if (amount === null || amount === undefined) return '—'
  return money.format(amount)
}

export function formatDate(iso) {
  if (!iso) return '—'
  return dateTime.format(new Date(iso))
}

export function timeUntil(iso) {
  if (!iso) return '—'
  const ms = new Date(iso).getTime() - Date.now()
  const abs = Math.abs(ms)
  const hours = Math.round(abs / 3_600_000)
  if (hours < 1) return ms > 0 ? 'due within the hour' : 'just went overdue'
  if (hours < 48) return ms > 0 ? `due in ${hours}h` : `${hours}h overdue`
  const days = Math.round(hours / 24)
  return ms > 0 ? `due in ${days}d` : `${days}d overdue`
}

export const STATUS_LABEL = {
  DRAFT: 'Draft',
  PENDING: 'Pending',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  EXPIRED: 'Expired',
  RECALLED: 'Withdrawn',
}
