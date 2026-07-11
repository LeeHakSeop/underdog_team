// stores/containerStore.js
import { defineStore } from 'pinia'
import { fetchContainers } from '@/api/adminApi/containerApi'

export const useContainerStore = defineStore('container', {
  state: () => ({ containers: [], loading: false, error: '' }),
  actions: {
    async loadContainers() {
      this.loading = true; this.error = ''
      try { this.containers = await fetchContainers() }
      catch (error) { this.error = '컨테이너 목록을 불러오지 못했습니다.'; throw error }
      finally { this.loading = false }
    },
  },
})
