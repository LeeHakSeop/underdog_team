import { request } from '../apiClient'
export const fetchGateLogs = () => request('/api/gate-log')
