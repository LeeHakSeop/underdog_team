// stores/taskStore.js
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
  },
})