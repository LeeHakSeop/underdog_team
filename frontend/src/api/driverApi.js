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
 * 운송사 담당자 승인
 */
export const approveDriverByCarrier = (userId) => {
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

export const withdrawDriver = (driverId) => {
  return request(`/api/driver/${driverId}/withdraw`, {
    method: 'PATCH',
  })
}

export const reactivateDriver = (driverId) => {
  return request(`/api/driver/${driverId}/reactivate`, {
    method: 'PATCH',
  })
}

/**
 * 기사 작업지시 조회(이름 기준)
 */
export const fetchMyWorkOrders = (userName) => {
  return request(
    `/api/driver/my-work-orders?userName=${encodeURIComponent(userName)}`
  )
}

/**
 * 기사 작업지시 조회(User ID 기준)
 */
export const fetchMyWorkOrdersByUserId = (userId) => {
  return request(`/api/driver/my-work-orders/user/${userId}`)
}
