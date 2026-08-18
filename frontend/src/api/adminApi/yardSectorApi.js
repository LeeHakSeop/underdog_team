import { request } from '../apiClient'

export const fetchYardSectors = () => request('/api/yard-sector')

export const updateYardSectorCapacity = (sectorId, capacity) => request(`/api/yard-sector/${sectorId}/capacity`, {
  method: 'PATCH',
  body: JSON.stringify({ capacity }),
})
