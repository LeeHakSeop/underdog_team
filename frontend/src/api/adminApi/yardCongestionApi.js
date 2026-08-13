import { request } from '../apiClient'

export const fetchYardCongestion = () => request('/api/yard-congestion')
