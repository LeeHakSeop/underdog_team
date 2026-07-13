// api/taskApi.js
import { request } from './apiClient'
export const fetchTasks = () => request('/api/task')
export const createTask = (task) => request('/api/task', { method: 'POST', body: JSON.stringify(task) })