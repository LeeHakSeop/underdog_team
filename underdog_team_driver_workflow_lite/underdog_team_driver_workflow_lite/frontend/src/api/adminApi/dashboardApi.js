import { request } from '../apiClient'

export const fetchAdminDashboard = () => {
  return request('/api/dashboard/admin')
}
