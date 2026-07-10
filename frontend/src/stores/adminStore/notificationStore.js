<<<<<<< HEAD
import { defineStore } from 'pinia'
import { fetchNotifications } from '@/api/adminApi/notificationApi'
=======
// stores/notificationStore.js
import { defineStore } from 'pinia'
import { fetchNotifications } from '@/api/notificationApi'
>>>>>>> origin/KBH

export const useNotificationStore = defineStore('notification', {
  state: () => ({ notifications: [], loading: false, error: '' }),
  actions: {
    async loadNotifications() {
      this.loading = true; this.error = ''
      try { this.notifications = await fetchNotifications() }
<<<<<<< HEAD
      catch (error) { this.error = '예외 알림 목록을 불러오지 못했습니다.'; throw error }
      finally { this.loading = false }
    },
  },
})
=======
      catch (error) { this.error = '알림 목록을 불러오지 못했습니다.'; throw error }
      finally { this.loading = false }
    },
  },
})
>>>>>>> origin/KBH
