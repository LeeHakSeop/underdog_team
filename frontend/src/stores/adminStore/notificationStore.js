import { defineStore } from 'pinia'
import { fetchNotifications, processNotification } from '@/api/adminApi/notificationApi'

const toList = (data) => {
  if (Array.isArray(data)) return data

  return data?.content || data?.items || data?.data || []
}

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [],
    loading: false,
    error: '',
    processingId: null,
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

    async processNotification(exceptionLogId, managerAction) {
      this.processingId = exceptionLogId
      this.error = ''

      try {
        await processNotification(exceptionLogId, {
          processStatus: 'PROCESSED',
          managerAction: managerAction || '관리자 확인 완료',
        })
        this.notifications = toList(await fetchNotifications())
      } catch (error) {
        this.error = error.message || '예외 처리 상태를 저장하지 못했습니다.'
        throw error
      } finally {
        this.processingId = null
      }
    },
  },
})
