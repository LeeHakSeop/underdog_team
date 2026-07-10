import { request } from './apiClient'

export const fetchCarriers = () => {
  return request('/api/carrier/list')
}

export const fetchCarrierById = (carrierId) => {
  return request(`/api/carrier/detail/${carrierId}`)
}

export const createCarrier = (carrier) => {
  return request('/api/carrier/reg', {
    method: 'POST',
    body: JSON.stringify(carrier),
  })
}

export const updateCarrier = (carrierId, carrier) => {
  return request(`/api/carrier/modify/${carrierId}`, {
    method: 'PUT',
    body: JSON.stringify(carrier),
  })
}

export const deleteCarrier = (carrierId) => {
  return request(`/api/carrier/delete/${carrierId}`, {
    method: 'DELETE',
  })
}
