import { request } from './apiClient'

export const loginApi = (loginData) => {
  return request('/api/login', {
    method: 'POST',
    body: JSON.stringify(loginData),
  })
}