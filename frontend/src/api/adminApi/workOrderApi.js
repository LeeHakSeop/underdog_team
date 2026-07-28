import { request } from '../apiClient'
export const fetchWorkOrders = () => request('/api/work-order')
export const fetchWorkStatusHistory = () => request('/api/work-order/history')
export const createWorkOrder = (workOrder) => request('/api/work-order', { method: 'POST', body: JSON.stringify(workOrder) })
export const updateWorkOrder = (workOrderId, workOrder) =>
  request(`/api/work-order/${workOrderId}`, { method: 'PUT', body: JSON.stringify(workOrder) })
export const deleteWorkOrder = (workOrderId) =>
  request(`/api/work-order/${workOrderId}`, { method: 'DELETE' })
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
