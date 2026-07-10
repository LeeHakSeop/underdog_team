import { defineStore } from 'pinia'
import { fetchAdminDashboard } from '@/api/adminApi/dashboardApi'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    dashboard: null,
    loading: false,
    error: '',
  }),

  actions: {
    async loadDashboard() {
      this.loading = true
      this.error = ''

      try {
        this.dashboard = await fetchAdminDashboard()
      } catch (error) {
        this.error = '대시보드 현황을 불러오지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})
