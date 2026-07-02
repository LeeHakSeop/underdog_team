import { createWorkOrder, fetchWorkOrders } from '@/api/adminApi/workOrderApi';
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
  },
})
