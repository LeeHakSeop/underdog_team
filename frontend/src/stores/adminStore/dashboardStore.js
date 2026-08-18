import { defineStore } from 'pinia'
import { fetchAdminDashboard } from '@/api/adminApi/dashboardApi'
import { fetchYardCongestion } from '@/api/adminApi/yardCongestionApi'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    dashboard: null,
    loading: false,
    error: '',
    lastUpdatedAt: null,
  }),

  actions: {
    async loadDashboard() {
      const firstLoad = this.dashboard === null

      if (firstLoad) {
        this.loading = true
      }
      this.error = ''

      try {
        const [dashboard, congestion] = await Promise.all([
          fetchAdminDashboard(),
          fetchYardCongestion(),
        ])
        const congestionSummary = congestion?.summary || {}

        this.dashboard = {
          ...dashboard,
          congestion,
          summary: {
            ...(dashboard?.summary || {}),
            waitingVehicles: congestionSummary.totalWaitingVehicleCount ?? dashboard?.summary?.waitingVehicles,
            congestedSectors: congestionSummary.dangerSectorCount ?? dashboard?.summary?.congestedSectors,
            warningSectors: congestionSummary.warningSectorCount ?? dashboard?.summary?.warningSectors,
          },
          sectorList: (congestion?.sectors || dashboard?.sectorList || []).slice(0, 8),
        }
        this.lastUpdatedAt = new Date().toISOString()
      } catch (error) {
        this.error = '대시보드 현황을 불러오지 못했습니다.'
        throw error
      } finally {
        if (firstLoad) {
          this.loading = false
        }
      }
    },
  },
})
