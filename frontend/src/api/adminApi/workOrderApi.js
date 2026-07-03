import { request } from '../apiClient'
export const fetchWorkOrders = () => request('/api/work-order')
export const createWorkOrder = (workOrder) => request('/api/work-order', { method: 'POST', body: JSON.stringify(workOrder) })
