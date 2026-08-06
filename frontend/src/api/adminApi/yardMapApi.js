import { request } from '../apiClient'

export const fetchYardMapSnapshot = () => request('/api/yard-map/snapshot')
