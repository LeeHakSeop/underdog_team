import { request } from './apiClient'

export const fetchCurrentWeather = () => {
  return request('/api/weather/current')
}
