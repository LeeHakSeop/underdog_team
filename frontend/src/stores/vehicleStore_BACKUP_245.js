import { defineStore } from 'pinia'
import { fetchVehicles } from '@/api/vehicleApi'

export const useVehicleStore = defineStore('vehicle', {
  state: () => ({
    vehicles: [],
    loading: false,
    error: '',
  }),

  actions: {
    async loadVehicles() {
      this.loading = true
      this.error = ''

      try {
        this.vehicles = await fetchVehicles()
      } catch (error) {
        this.error = '차량 목록을 불러오지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    
  },
})

