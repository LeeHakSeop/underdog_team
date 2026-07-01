// api/vehicleApi.js
import { request } from './apiClient'

export const fetchVehicles = () => {
  return request('/api/vehicle')
}