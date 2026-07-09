import { request } from './apiClient'

export const fetchDrivers = () => {
  return request('/api/driver/list')
}

export const fetchDriverById = (driverId) => {
  return request(`/api/driver/detail/${driverId}`)
}

export const createDriver = (driver) => {
  return request('/api/driver/reg', {
    method: 'POST',
    body: JSON.stringify(driver),
  })
}

/**
 * 운송사 승인
 */
export const carrierApproveDriver = (userId) => {
  return request(`/api/driver/${userId}/carrier-approve`, {
    method: 'PATCH',
  })
}

export const updateDriver = (driverId, driver) => {
  return request(`/api/driver/modify/${driverId}`, {
    method: 'PUT',
    body: JSON.stringify(driver),
  })
}

export const deleteDriver = (driverId) => {
  return request(`/api/driver/delete/${driverId}`, {
    method: 'DELETE',
  })
}

export const approveDriverByCarrier = (userId) => {
  return request(`/api/driver/${userId}/carrier-approve`, {
    method: 'PATCH',
  })
}

