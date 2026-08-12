const requestJson = async (url, errorMessage) => {
  const response = await fetch(url)
  if (!response.ok) throw new Error(errorMessage)
  return response.json()
}

export const fetchPredictiveEquipment = () =>
  requestJson('/api/predictive-maintenance/equipment', '안테나 목록을 불러오지 못했습니다.')

export const fetchPredictiveSensorData = (equipmentCode) =>
  requestJson(
    `/api/predictive-maintenance/sensor-data?equipmentCode=${encodeURIComponent(equipmentCode)}`,
    `${equipmentCode} 센서 데이터를 불러오지 못했습니다.`,
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

export const fetchPredictiveMetadata = () =>
  requestJson(
    '/data/predictive-maintenance-metadata.json',
    '예지보전 표시 설정을 불러오지 못했습니다.',
  )
