import { defineStore } from 'pinia'
import { fetchCarriers, createCarrier } from '@/api/carrierApi'

export const useCarrierStore = defineStore('carrier', {
  state: () => ({
    carriers: [],
    loading: false,
    error: '',
  }),

  actions: {
    async loadCarriers() {
      this.loading = true
      this.error = ''

      try {
        this.carriers = await fetchCarriers()
      } catch (error) {
        this.error = '운송사 목록을 불러오지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async addCarrier(carrier) {
      this.loading = true
      this.error = ''

      try {
        await createCarrier(carrier)
        await this.loadCarriers()
      } catch (error) {
        this.error = '운송사 등록에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})