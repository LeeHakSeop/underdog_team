import { defineStore } from 'pinia'
import { recognizePlate } from '@/api/adminApi/plateRecognitionApi'

export const usePlateRecognitionStore = defineStore('plateRecognition', {
  state: () => ({
    result: null,
    tractorResult: null,
    trailerResult: null,
    loading: false,
    tractorLoading: false,
    trailerLoading: false,
    error: '',
  }),
  actions: {
    reset() {
      this.result = null
      this.tractorResult = null
      this.trailerResult = null
      this.loading = false
      this.tractorLoading = false
      this.trailerLoading = false
      this.error = ''
    },
    async recognize(file, ocrType = 'crnn', targetType = 'trailer') {
      this.loading = true
      this.error = ''
      this.result = null
      const plateType = targetType === 'tractor' ? 'TRACTOR' : 'TRAILER'

      if (targetType === 'tractor') {
        this.tractorLoading = true
        this.tractorResult = null
      } else {
        this.trailerLoading = true
        this.trailerResult = null
      }

      try {
        const result = await recognizePlate(file, ocrType, plateType)

        this.result = result

        if (targetType === 'tractor') {
          this.tractorResult = result
        } else {
          this.trailerResult = result
        }
      } catch (error) {
        this.error = error.message || '번호판 인식 요청에 실패했습니다.'
        throw error
      } finally {
        this.loading = false
        this.tractorLoading = false
        this.trailerLoading = false
      }
    },
  },
})
