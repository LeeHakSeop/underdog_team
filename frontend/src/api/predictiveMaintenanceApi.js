import { request } from '@/api/apiClient'
import predictiveMaintenanceMetadata from '@/data/predictive-maintenance-metadata.json'

const requestJson = async (url, errorMessage) => {
  try {
    return await request(url)
  } catch (error) {
    if (error?.message === '로그인이 필요합니다.') {
      throw error
    }

    throw new Error(error?.message || errorMessage)
  }
}

export const fetchPredictiveEquipment = () =>
  requestJson('/api/predictive-maintenance/equipment', '게이트 설비 목록을 불러오지 못했습니다.')

export const fetchPredictiveSensorData = (equipmentCode) =>
  requestJson(
    `/api/predictive-maintenance/sensor-data?equipmentCode=${encodeURIComponent(equipmentCode)}`,
    `${equipmentCode} 상태 지표 데이터를 불러오지 못했습니다.`,
  )

export const fetchPredictiveEvents = (equipmentCode = '') => {
  const query = equipmentCode
    ? `?equipmentCode=${encodeURIComponent(equipmentCode)}`
    : ''
  return requestJson(
    `/api/predictive-maintenance/events${query}`,
    '고장·정비 기록을 불러오지 못했습니다.',
  )
}

export const fetchPredictiveMetadata = async () => predictiveMaintenanceMetadata
