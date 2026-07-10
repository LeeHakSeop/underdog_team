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

export const deleteDriver = (driverId) => {
  return request(`/api/driver/delete/${driverId}`, {
    method: 'DELETE',
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
/**
 * 운송사 기사관리 목록 조회
 * 운송사 계정 userId를 기준으로 소속 기사와 가입 시 등록한 트랙터 정보를 조회한다.
 */
export const fetchCarrierDriverManagement = (carrierUserId) => {
  return request(`/api/driver/carrier/${carrierUserId}/management`)
}
