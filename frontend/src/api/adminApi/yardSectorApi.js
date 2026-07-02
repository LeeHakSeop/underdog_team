import { request } from '../apiClient'

export const fetchYardSectors = () => request('/api/yard-sector')
