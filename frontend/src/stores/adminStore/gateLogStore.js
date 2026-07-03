// stores/gateLogStore.js
import { defineStore } from 'pinia'
import { fetchGateLogs } from '@/api/adminApi/gateLogApi'

export const useGateLogStore = defineStore('gateLog', {
  state: () => ({ gateLogs: [], loading: false, error: '' }),
  actions: {
    async loadGateLogs() {
      this.loading = true; this.error = ''
      try { this.gateLogs = await fetchGateLogs() }
      catch (error) { this.error = '게이트 출입 이력을 불러오지 못했습니다.'; throw error }
      finally { this.loading = false }
    },
  },
})
