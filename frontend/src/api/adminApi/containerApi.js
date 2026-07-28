// api/containerApi.js
import { request } from '../apiClient'
export const fetchContainers = () => request('/api/container')
export const createContainer = (container) => request('/api/container', {
  method: 'POST',
  body: JSON.stringify(container),
})
export const updateContainer = (containerId, container) => request(`/api/container/${containerId}`, {
  method: 'PUT',
  body: JSON.stringify(container),
})
export const deleteContainer = (containerId) => request(`/api/container/${containerId}`, {
  method: 'DELETE',
})
