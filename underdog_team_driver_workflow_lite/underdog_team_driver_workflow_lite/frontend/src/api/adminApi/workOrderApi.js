import { request } from '../apiClient'

export const fetchWorkOrders = () => request('/api/work-order')
export const fetchCarrierWorkOrders = (userId) => request(`/api/work-order/carrier/user/${userId}`)
export const createWorkOrder = (workOrder) => request('/api/work-order', {
  method: 'POST',
  body: JSON.stringify(workOrder),
})
export const approveWorkOrder = (workOrderId) => request(`/api/work-order/${workOrderId}/approve`, {
  method: 'PATCH',
})
export const updateWorkOrderStatus = (workOrderId, workStatus) => request(`/api/work-order/${workOrderId}/status`, {
  method: 'PATCH',
  body: JSON.stringify({ workStatus }),
})
export const fetchTrailerWorkInfo = (vehicleId) => request(`/api/work-order/trailer-info/${vehicleId}`)
