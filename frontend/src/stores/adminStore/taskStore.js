<<<<<<< HEAD:frontend/src/stores/adminStore/workOrderStore.js
import {
  approveWorkOrder,
  completeWorkOrder,
  createWorkOrder,
  fetchWorkOrders,
  startWorkOrder,
} from '@/api/adminApi/workOrderApi'
=======
// stores/taskStore.js
>>>>>>> origin/KBH:frontend/src/stores/adminStore/taskStore.js
import { defineStore } from 'pinia'
import { fetchTasks, createTask } from '@/api/taskApi'

export const useTaskStore = defineStore('task', {
  state: () => ({ tasks: [], loading: false, error: '' }),
  actions: {
    async loadTasks() {
      this.loading = true; this.error = ''
      try { this.tasks = await fetchTasks() }
      catch (error) { this.error = '작업 목록을 불러오지 못했습니다.'; throw error }
      finally { this.loading = false }
    },
    async addTask(task) {
      this.loading = true; this.error = ''
      try { await createTask(task); await this.loadTasks() }
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