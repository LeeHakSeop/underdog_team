import { defineStore } from 'pinia'
import { fetchYardSectors } from '@/api/adminApi/yardSectorApi'

export const useYardSectorStore = defineStore('yardSector', {
  state: () => ({ yardSectors: [], loading: false, error: '' }),
  actions: {
    async loadYardSectors() {
      this.loading = true
      this.error = ''
      try {
        this.yardSectors = await fetchYardSectors()
      } catch (error) {
        this.error = '야드 섹터 목록을 불러오지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})
