import { request } from '../apiClient'

export const fetchGateLogs = () => {
  return request('/api/gate-log')
}

export const processGate = (payload) => {
  return request('/api/gate-log/process', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}