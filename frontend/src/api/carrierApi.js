// src/api/carrierApi.js
import { request } from './apiClient'

export const fetchCarriers = () => {
  return request('/api/carrier')
}

export const fetchCarrierById = (carrierId) => {
  return request(`/api/carrier/${carrierId}`)
}

export const createCarrier = (carrier) => {
  return request('/api/carrier', {
    method: 'POST',
    body: JSON.stringify(carrier),
  })
}

export const updateCarrier = (carrierId, carrier) => {
  return request(`/api/carrier/${carrierId}`, {
    method: 'PUT',
    body: JSON.stringify(carrier),
  })
}

export const deleteCarrier = (carrierId) => {
  return request(`/api/carrier/${carrierId}`, {
    method: 'DELETE',
  })
}