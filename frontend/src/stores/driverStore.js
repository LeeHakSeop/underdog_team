// stores/driverStore.js
import { defineStore } from 'pinia'
import { fetchDrivers, createDriver } from '@/api/driverApi'

export const useDriverStore = defineStore('driver', {
  state: () => ({
    drivers: [],
    loading: false,
    error: '',
  }),

  actions: {
    async loadDrivers() {
      this.loading = true
      this.error = ''

      try {
        this.drivers = await fetchDrivers()
      } catch (error) {
        this.error = '기사 목록을 불러오지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async addDriver(driver) {
      this.loading = true
      this.error = ''

      try {
        await createDriver(driver)
        await this.loadDrivers()
      } catch (error) {
        this.error = '기사 등록에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})