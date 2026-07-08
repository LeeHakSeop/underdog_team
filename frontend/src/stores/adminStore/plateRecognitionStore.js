import { defineStore } from 'pinia'
import { recognizePlate } from '@/api/adminApi/plateRecognitionApi'

export const usePlateRecognitionStore = defineStore('plateRecognition', {
  state: () => ({
    result: null,
    loading: false,
    error: '',
  }),
  actions: {
    async recognize(file) {
      this.loading = true
      this.error = ''
      this.result = null

      try {
        this.result = await recognizePlate(file)
      } catch (error) {
        this.error = error.message || '번호판 인식 요청에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})
