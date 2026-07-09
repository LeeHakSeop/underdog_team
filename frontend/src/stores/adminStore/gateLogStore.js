// stores/gateLogStore.js
import { defineStore } from 'pinia'
import { fetchGateLogs, processGate } from '@/api/adminApi/gateLogApi'

export const useGateLogStore = defineStore('gateLog', {
  state: () => ({ gateLogs: [], processResult: null, loading: false, error: '' }),
  actions: {
    async loadGateLogs() {
      this.loading = true; this.error = ''
      try { this.gateLogs = await fetchGateLogs() }
      catch (error) { this.error = '게이트 출입 이력을 불러오지 못했습니다.'; throw error }
      finally { this.loading = false }
    },

    async processGate(payload) {
      this.loading = true; this.error = ''; this.processResult = null
      try {
        this.processResult = await processGate(payload)
        await this.loadGateLogs()
        return this.processResult
      } catch (error) {
        this.error = '최종 출입 처리를 진행하지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})
