export const predictiveMaintenanceTypes = [
  {
    code: 'ANTENNA',
    label: '안테나 예지보전',
    description: '안테나별 센서 이상과 지속시간을 바탕으로 운영 상태와 고장 전조를 확인합니다.',
    enabled: true,
  },
  {
    code: 'TYPE_2',
    label: '두 번째 설비',
    description: '데이터 계약이 확정되면 같은 화면 구조에 연결합니다.',
    enabled: false,
  },
]

export const predictiveMaintenanceSections = [
  { code: 'dashboard', label: '대시보드' },
  { code: 'maintenance', label: '점검·정비' },
]

// VMS와 실제 모델이 연결되는 베타 단계에서 다시 활성화할 메뉴입니다.
export const predictiveMaintenanceFutureSections = [
  { code: 'equipment', label: '설비 현황' },
  { code: 'readings', label: '센서 데이터' },
  { code: 'alerts', label: '경보 관리' },
  { code: 'models', label: '모델 정보' },
]

export const predictiveRiskLabels = {
  NORMAL: '정상',
  CAUTION: '주의',
  DANGER: '위험',
}
