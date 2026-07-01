// api/driverApi.js
import { request } from './apiClient'

export const fetchDrivers = () => {
  return request('/api/driver')
}

export const fetchDriverById = (driverId) => {
  return request(`/api/driver/${driverId}`)
}

export const createDriver = (driver) => {
  return request('/api/driver', {
    method: 'POST',
    body: JSON.stringify(driver),
  })
}

export const updateDriver = (driverId, driver) => {
  return request(`/api/driver/${driverId}`, {
    method: 'PUT',
    body: JSON.stringify(driver),
  })
}

export const deleteDriver = (driverId) => {
  return request(`/api/driver/${driverId}`, {
    method: 'DELETE',
  })
}