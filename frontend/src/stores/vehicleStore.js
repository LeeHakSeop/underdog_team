// stores/vehicleStore.js
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

    // 백엔드에 등록(POST) API가 없어 현재는 동작하지 않습니다.
    // addVehicle은 VehicleController에 @PostMapping이 추가된 뒤 구현하는 것이 맞습니다.
  },
})