// api/memberApi.js
import { request } from './apiClient'
export const fetchMembers = () => request('/api/member')
export const createMember = (member) => request('/api/member', { method: 'POST', body: JSON.stringify(member) })
export const updateMember = (memberId, member) => request(`/api/member/${memberId}`, { method: 'PUT', body: JSON.stringify(member) })
export const deleteMember = (memberId) => request(`/api/member/${memberId}`, { method: 'DELETE' })