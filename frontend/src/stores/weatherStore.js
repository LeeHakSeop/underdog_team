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
        const weather = await fetchCurrentWeather()
        this.weatherInfo = weather
        this.lastFetchedAt = new Date().toISOString()

        if (!weather?.available) {
          this.errMsg = weather?.errorMessage || weather?.message || '부산항 날씨 정보를 확인할 수 없습니다.'
        }
      } catch (error) {
        this.errMsg = error?.message || '부산항 날씨 정보를 불러오지 못했습니다.'

        if (this.weatherInfo?.available) {
          this.weatherInfo = {
            ...this.weatherInfo,
            stale: true,
            fallbackUsed: true,
            errorMessage: '실시간 기상 호출에 실패해 마지막 정상 데이터를 유지합니다.',
          }
        }
      } finally {
        if (firstLoad) {
          this.loading = false
        }
      }
    },
  },
})
