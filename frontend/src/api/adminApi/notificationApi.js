import { request } from '../apiClient'

export const fetchNotifications = () => request('/api/exception-log')

export const processNotification = (exceptionLogId, payload) => request(`/api/exception-log/${exceptionLogId}/process`, {
  method: 'PUT',
  body: payload,
})
