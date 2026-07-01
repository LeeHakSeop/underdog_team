// api/containerApi.js
import { request } from './apiClient'
export const fetchContainers = () => request('/api/container')