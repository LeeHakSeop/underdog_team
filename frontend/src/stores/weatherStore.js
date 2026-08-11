import { defineStore } from 'pinia'
import { fetchCurrentWeather } from '@/api/weatherApi'

export const useWeatherStore = defineStore('weather', {
  state: () => ({
    weatherInfo: null,
    loading: false,
    errMsg: '',
    lastFetchedAt: null,
  }),

  actions: {
    async fetchWeather() {
      const firstLoad = this.weatherInfo === null

      if (firstLoad) {
        this.loading = true
      }
      this.errMsg = ''

      try {
        this.weatherInfo = await fetchCurrentWeather()
        this.lastFetchedAt = new Date().toISOString()
      } catch (error) {
        this.errMsg = '날씨 정보를 불러오지 못했습니다.'
        throw error
      } finally {
        if (firstLoad) {
          this.loading = false
        }
      }
    },
  },
})
