export const predictiveMaintenanceSections = [
  { code: 'dashboard', label: '대시보드' },
  { code: 'maintenance', label: '점검·정비' },
]

// VMS와 실제 모델이 연결되는 베타 단계에서 다시 활성화할 메뉴입니다.
export const predictiveMaintenanceFutureSections = [
  { code: 'equipment', label: '설비 현황' },
  { code: 'readings', label: '상태 지표 데이터' },
  { code: 'alerts', label: '경보 관리' },
  { code: 'models', label: '모델 정보' },
]

export const predictiveRiskLabels = {
  NORMAL: '정상',
  CAUTION: '주의',
  DANGER: '위험',
}
