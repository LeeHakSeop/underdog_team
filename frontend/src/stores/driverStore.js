// stores/driverStore.js
import { defineStore } from 'pinia'
import {
  approveDriverByCarrier,
  createDriver,
  reactivateDriver as reactivateDriverApi,
  fetchDrivers,
  fetchMyWorkOrders,
  fetchMyWorkOrdersByUserId,
  withdrawDriver as withdrawDriverApi,
  updateDriver,
} from '@/api/driverApi'

export const useDriverStore = defineStore('driver', {
  state: () => ({
    drivers: [],
    myWorkOrders: [],
    workOrdersLoaded: false,
    loading: false,
    error: '',
  }),

  actions: {
    async loadDrivers() {
      this.loading = true
      this.error = ''

      try {
        this.drivers = (await fetchDrivers()) || []
      } catch (error) {
        this.error = error.message || '기사 목록을 불러오지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async loadMyWorkOrders(userName) {
      if (!this.workOrdersLoaded) {
        this.loading = true
      }
      this.error = ''

      try {
        this.myWorkOrders = (await fetchMyWorkOrders(userName)) || []
        this.workOrdersLoaded = true
      } catch (error) {
        this.error = error.message || '작업정보를 불러오지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async loadMyWorkOrdersByUserId(userId) {
      if (!this.workOrdersLoaded) {
        this.loading = true
      }
      this.error = ''

      try {
        this.myWorkOrders = (await fetchMyWorkOrdersByUserId(userId)) || []
        this.workOrdersLoaded = true
      } catch (error) {
        this.error = error.message || '작업정보를 불러오지 못했습니다.'
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
        this.error = error.message || '기사 등록에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async editDriver(driverId, driver) {
      this.loading = true
      this.error = ''

      try {
        await updateDriver(driverId, driver)
        await this.loadDrivers()
      } catch (error) {
        this.error = error.message || '기사 수정에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async carrierApproveDriver(userId) {
      this.loading = true
      this.error = ''

      try {
        await approveDriverByCarrier(userId)
        await this.loadDrivers()
      } catch (error) {
        this.error = error.message || '기사 승인에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async withdrawDriver(driverId) {
      this.loading = true
      this.error = ''

      try {
        await withdrawDriverApi(driverId)
        await this.loadDrivers()
      } catch (error) {
        this.error = error.message || '기사 탈퇴 처리에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async reactivateDriver(driverId) {
      this.loading = true
      this.error = ''

      try {
        await reactivateDriverApi(driverId)
        await this.loadDrivers()
      } catch (error) {
        this.error = error.message || '기사 재활성화에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})
