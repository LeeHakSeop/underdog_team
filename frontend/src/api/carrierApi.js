// src/api/carrierApi.js

import { request } from './apiClient'

/**
 * 운송사 목록 조회
 */
export const fetchCarriers = () => {
  return request('/api/carrier/list')
}

/**
 * 운송사 상세조회
 */
export const fetchCarrierById = (carrierId) => {
  return request(`/api/carrier/detail/${carrierId}`)
}

/**
 * 운송사 등록
 */
export const createCarrier = (carrier) => {
  return request('/api/carrier/reg', {
    method: 'POST',
    body: JSON.stringify(carrier),
  })
}

/**
 * 운송사 수정
 */
export const updateCarrier = (carrierId, carrier) => {
  return request(`/api/carrier/modify/${carrierId}`, {
    method: 'PUT',
    body: JSON.stringify(carrier),
  })
}

/**
 * 운송사 삭제
 */
export const deleteCarrier = (carrierId) => {
  return request(`/api/carrier/delete/${carrierId}`, {
    method: 'DELETE',
  })
}
