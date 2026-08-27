const profiles = {
  GAT: { type: 'GATE_RECOGNITION', label: '게이트 자동인식 장치' },
  QC: { type: 'QUAY_CRANE', label: '안벽 컨테이너 크레인 제어장치' },
  TC: { type: 'TRANSFER_CRANE', label: '트랜스퍼 크레인 제어장치' },
  YT: { type: 'YARD_TRACTOR', label: '야드 트랙터 운행 제어장치' },
  DEMO: { type: 'DEMO', label: '시연용 트랜스퍼 크레인 제어장치' },
}

export const getPortEquipmentProfile = (equipmentCode = '') => {
  const prefix = String(equipmentCode).split('-')[0]
  return profiles[prefix] || { type: 'PORT_EQUIPMENT', label: '항만 운영장비' }
}

export const formatPortEquipment = (equipmentCode = '') => {
  if (!equipmentCode) return '장비 미선택'
  const profile = getPortEquipmentProfile(equipmentCode)
  return `${equipmentCode} · ${profile.label}`
}

export const portEquipmentTypeOptions = [
  { code: 'ALL', label: '전체 장비' },
  ...Object.entries(profiles)
    .filter(([prefix]) => prefix !== 'DEMO')
    .map(([prefix, profile]) => ({ code: profile.type, prefix, label: profile.label })),
]
