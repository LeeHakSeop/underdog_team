import { defineStore } from 'pinia'
import { fetchYardCongestion } from '@/api/adminApi/yardCongestionApi'

export const useYardCongestionStore = defineStore('yardCongestion', {
  state: () => ({
    summary: null,
    sectors: [],
    loading: false,
    error: '',
    lastUpdatedAt: null,
  }),

  getters: {
    sectorById: (state) => state.sectors.reduce((items, sector) => {
      items.set(sector.sectorId, sector)
      return items
    }, new Map()),
  },

  actions: {
    async loadCongestion() {
      this.loading = true
      this.error = ''

      try {
        const congestion = await fetchYardCongestion()
        this.summary = congestion?.summary || null
        this.sectors = congestion?.sectors || []
        this.lastUpdatedAt = new Date().toISOString()
      } catch (error) {
        this.error = error.message || '야드 혼잡도 데이터를 불러오지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})
