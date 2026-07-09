// api/gateLogApi.js
import { request } from './apiClient'
export const fetchGateLogs = () => request('/api/gatelog')