// stores/containerStore.js
import { defineStore } from 'pinia'
import {
  createContainer,
  deleteContainer,
  fetchContainers,
  updateContainer,
} from '@/api/adminApi/containerApi'

export const useContainerStore = defineStore('container', {
  state: () => ({ containers: [], loading: false, error: '' }),
  actions: {
    async loadContainers() {
      this.loading = true; this.error = ''
      try { this.containers = await fetchContainers() }
      catch (error) { this.error = '컨테이너 목록을 불러오지 못했습니다.'; throw error }
      finally { this.loading = false }
    },
    async addContainer(container) {
      this.loading = true; this.error = ''
      try {
        await createContainer(container)
        await this.loadContainers()
      } catch (error) {
        this.error = error.message || '컨테이너 등록에 실패했습니다.'
        throw error
      } finally { this.loading = false }
    },
    async editContainer(containerId, container) {
      this.loading = true; this.error = ''
      try {
        await updateContainer(containerId, container)
        await this.loadContainers()
      } catch (error) {
        this.error = error.message || '컨테이너 수정에 실패했습니다.'
        throw error
      } finally { this.loading = false }
    },
    async removeContainer(containerId) {
      this.loading = true; this.error = ''
      try {
        await deleteContainer(containerId)
        await this.loadContainers()
      } catch (error) {
        this.error = error.message || '컨테이너 삭제에 실패했습니다.'
        throw error
      } finally { this.loading = false }
    },
  },
})
