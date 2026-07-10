// stores/vehicleStore.js
import { defineStore } from 'pinia'
import {
  createVehicle,
  deleteVehicle,
  fetchVehicles,
  updateVehicle,
  updateVehicleApproval,
} from '@/api/vehicleApi'

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
        this.vehicles = (await fetchVehicles()) || []
      } catch (error) {
        this.error = error.message || '차량 목록을 불러오지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async addVehicle(vehicle) {
      this.loading = true
      this.error = ''

      try {
        await createVehicle(vehicle)
        await this.loadVehicles()
      } catch (error) {
        this.error = error.message || '차량 등록에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async editVehicle(vehicleId, vehicle) {
      this.loading = true
      this.error = ''

      try {
        await updateVehicle(vehicleId, vehicle)
        await this.loadVehicles()
      } catch (error) {
        this.error = error.message || '차량 수정에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async approveVehicle(vehicleId, isRegistered) {
      this.loading = true
      this.error = ''

      try {
        await updateVehicleApproval(vehicleId, isRegistered)
        await this.loadVehicles()
      } catch (error) {
        this.error = error.message || '차량 승인 처리에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async removeVehicle(vehicleId) {
      this.loading = true
      this.error = ''

      try {
        await deleteVehicle(vehicleId)
        await this.loadVehicles()
      } catch (error) {
        this.error = error.message || '차량 삭제에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})