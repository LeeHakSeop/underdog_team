// stores/driverStore.js
import { defineStore } from 'pinia'
import {
  completeMyWorkOrder,
  approveDriverByCarrier,
  createDriver,
  fetchMyWorkStatusHistory,
  reactivateDriver as reactivateDriverApi,
  fetchDrivers,
  fetchMyWorkOrders,
  fetchMyWorkOrdersByUserId,
  startMyWorkOrder,
  withdrawDriver as withdrawDriverApi,
  updateDriver,
} from '@/api/driverApi'

export const useDriverStore = defineStore('driver', {
  state: () => ({
    drivers: [],
    myWorkOrders: [],
    workHistory: [],
    workOrdersLoaded: false,
    historyLoaded: false,
    loading: false,
    error: '',
    actionMessage: '',
    actionResult: null,
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

    async loadMyWorkHistory(userId) {
      if (!this.historyLoaded) {
        this.loading = true
      }
      this.error = ''

      try {
        this.workHistory = (await fetchMyWorkStatusHistory(userId)) || []
        this.historyLoaded = true
      } catch (error) {
        this.error = error.message || '작업 이력을 불러오지 못했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },

    async startAssignedWork(workOrderId, userId) {
      this.loading = true
      this.error = ''
      this.actionMessage = ''
      this.actionResult = null

      try {
        const result = await startMyWorkOrder(workOrderId)
        if (result?.success === false) {
          this.actionResult = result
          this.error = result.message || '작업 시작 처리에 실패했습니다.'
          return result
        }
        this.actionResult = result
        this.actionMessage = result?.message || '작업이 시작되었습니다.'
        await Promise.all([
          this.loadMyWorkOrdersByUserId(userId),
          this.loadMyWorkHistory(userId),
        ])
        return result
      } catch (error) {
        this.error = error.message || '작업 시작 처리에 실패했습니다.'
        this.actionResult = {
          success: false,
          exceptionType: 'WORK_ORDER_REQUEST_FAILED',
          message: this.error,
        }
        throw error
      } finally {
        this.loading = false
      }
    },

    async completeAssignedWork(workOrderId, userId) {
      this.loading = true
      this.error = ''
      this.actionMessage = ''
      this.actionResult = null

      try {
        const result = await completeMyWorkOrder(workOrderId)
        if (result?.success === false) {
          this.actionResult = result
          this.error = result.message || '작업 완료 처리에 실패했습니다.'
          return result
        }
        this.actionResult = result
        this.actionMessage = result?.message || '작업이 완료되었습니다.'
        await Promise.all([
          this.loadMyWorkOrdersByUserId(userId),
          this.loadMyWorkHistory(userId),
        ])
        return result
      } catch (error) {
        this.error = error.message || '작업 완료 처리에 실패했습니다.'
        this.actionResult = {
          success: false,
          exceptionType: 'WORK_ORDER_REQUEST_FAILED',
          message: this.error,
        }
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
