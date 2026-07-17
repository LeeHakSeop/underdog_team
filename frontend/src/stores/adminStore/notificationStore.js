import { defineStore } from 'pinia'
import { fetchNotifications } from '@/api/adminApi/notificationApi'

const toList = (data) => {
  if (Array.isArray(data)) return data

  return data?.content || data?.items || data?.data || []
}

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [],
    loading: false,
    error: '',
  }),

  actions: {
    async loadNotifications() {
      this.loading = true
      this.error = ''

      try {
        this.notifications = toList(await fetchNotifications())
      } catch (error) {
        this.error = error.message || '예외 알림 목록을 불러오지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})
