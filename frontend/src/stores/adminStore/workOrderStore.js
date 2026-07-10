import {
  approveWorkOrder,
  completeWorkOrder,
  createWorkOrder,
  fetchWorkOrders,
  startWorkOrder,
} from '@/api/adminApi/workOrderApi'
import { defineStore } from 'pinia'

export const useWorkOrderStore = defineStore('workOrder', {
  state: () => ({ workOrders: [], loading: false, error: '' }),
  actions: {
    async loadWorkOrders() {
      this.loading = true; this.error = ''
      try { this.workOrders = await fetchWorkOrders() }
      catch (error) { this.error = '작업 목록을 불러오지 못했습니다.'; throw error }
      finally { this.loading = false }
    },
    async addWorkOrder(workOrder) {
      this.loading = true; this.error = ''
      try { await createWorkOrder(workOrder); await this.loadWorkOrders() }
      catch (error) { this.error = '작업 등록에 실패했습니다.'; throw error }
      finally { this.loading = false }
    },
    async approve(workOrderId) {
      this.error = ''
      try { await approveWorkOrder(workOrderId); await this.loadWorkOrders() }
      catch (error) { this.error = error.message || '작업 승인에 실패했습니다.'; throw error }
    },
    async start(workOrderId) {
      this.error = ''
      try { await startWorkOrder(workOrderId); await this.loadWorkOrders() }
      catch (error) { this.error = error.message || '작업 시작 처리에 실패했습니다.'; throw error }
    },
    async complete(workOrderId) {
      this.error = ''
      try { await completeWorkOrder(workOrderId); await this.loadWorkOrders() }
      catch (error) { this.error = error.message || '작업 완료 처리에 실패했습니다.'; throw error }
    },
  },
})
