// stores/memberStore.js
import { defineStore } from 'pinia'
import { fetchMembers, createMember } from '@/api/memberApi'

export const useMemberStore = defineStore('member', {
  state: () => ({ members: [], loading: false, error: '' }),
  actions: {
    async loadMembers() {
      this.loading = true; this.error = ''
      try { this.members = await fetchMembers() }
      catch (error) { this.error = '가입 인원 목록을 불러오지 못했습니다.'; throw error }
      finally { this.loading = false }
    },
    async addMember(member) {
      this.loading = true; this.error = ''
      try { await createMember(member); await this.loadMembers() }
      catch (error) { this.error = '가입 인원 등록에 실패했습니다.'; throw error }
      finally { this.loading = false }
    },
  },
})