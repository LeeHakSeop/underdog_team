import { request } from '../apiClient'

export const fetchNotifications = () => request('/api/exception-log')
