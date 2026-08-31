import { request } from '@/api/apiClient'
import predictiveMaintenanceMetadata from '@/data/predictive-maintenance-metadata.json'

const mapLegacyEquipmentCode = (equipmentCode) => {
  const match = /^ANT-(\d{3})$/.exec(equipmentCode)
  if (!match) return equipmentCode

  const number = Number(match[1])
  if (number <= 4) return `GAT-${String(number).padStart(3, '0')}`
  if (number <= 12) return `QC-${String(number - 4).padStart(3, '0')}`
  if (number <= 20) return `TC-${String(number - 12).padStart(3, '0')}`
  return `YT-${String(number - 20).padStart(3, '0')}`
}

const normalizedPredictiveMetadata = {
  ...predictiveMaintenanceMetadata,
  sensorLimits: Object.fromEntries(
    Object.entries(predictiveMaintenanceMetadata.sensorLimits || {}).map(([equipmentCode, limits]) => [
      mapLegacyEquipmentCode(equipmentCode),
      limits,
    ]),
  ),
}

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
  requestJson('/api/predictive-maintenance/equipment', '항만 장비 목록을 불러오지 못했습니다.')

export const fetchPredictiveSensorData = (equipmentCode) =>
  requestJson(
    `/api/predictive-maintenance/sensor-data?equipmentCode=${encodeURIComponent(equipmentCode)}`,
    `${equipmentCode} 상태 지표 데이터를 불러오지 못했습니다.`,
  )

export const fetchLatestPredictiveSensorData = () =>
  requestJson('/api/predictive-maintenance/sensor-data/latest', '최신 장비 상태를 불러오지 못했습니다.')

export const fetchPredictiveEvents = (equipmentCode = '') => {
  const query = equipmentCode
    ? `?equipmentCode=${encodeURIComponent(equipmentCode)}`
    : ''
  return requestJson(
    `/api/predictive-maintenance/events${query}`,
    '고장·정비 기록을 불러오지 못했습니다.',
  )
}

export const fetchPredictiveMetadata = async () => normalizedPredictiveMetadata
