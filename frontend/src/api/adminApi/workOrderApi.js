import { request } from '../apiClient'
export const fetchWorkOrders = () => request('/api/work-order')
export const createWorkOrder = (workOrder) => request('/api/work-order', { method: 'POST', body: JSON.stringify(workOrder) })
export const approveWorkOrder = (workOrderId) =>
  request(`/api/work-order/${workOrderId}/approve`, { method: 'PATCH' })
export const rejectWorkOrder = (workOrderId) =>
  request(`/api/work-order/${workOrderId}/reject`, { method: 'PATCH' })
export const startWorkOrder = (workOrderId) =>
  request(`/api/work-order/${workOrderId}/start`, { method: 'PATCH' })
export const completeWorkOrder = (workOrderId) =>
  request(`/api/work-order/${workOrderId}/complete`, { method: 'PATCH' })
export const fetchTrailerWorkInfo = (vehicleId) =>
  request(`/api/work-order/trailer-info/${vehicleId}`)
