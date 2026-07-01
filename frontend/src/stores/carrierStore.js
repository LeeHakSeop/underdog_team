import { defineStore } from 'pinia'
import { createCarrier, deleteCarrier, fetchCarriers, updateCarrier } from '@/api/carrierApi'

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

    async editCarrier(carrierId, carrier) {
      this.loading = true
      this.error = ''

      try {
        await updateCarrier(carrierId, carrier)
        await this.loadCarriers()
      } catch (error) {
        this.error = '운송사 수정에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async removeCarrier(carrierId) {
      this.loading = true
      this.error = ''

      try {
        await deleteCarrier(carrierId)
        await this.loadCarriers()
      } catch (error) {
        this.error = '운송사 삭제에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})
