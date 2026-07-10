import { request } from '../apiClient'
export const fetchGateLogs = () => request('/api/gate-log')
export const processGate = (payload) => request('/api/gate-log/process', {
  method: 'POST',
  body: JSON.stringify(payload),
})
