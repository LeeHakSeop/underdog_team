const LABELS = {
  work: {
    DISPATCH_WAITING: { label: '승인 대기', tone: 'amber' },
    APPROVED: { label: '입차 대기', tone: 'blue' },
    GATE_IN: { label: '입차 완료', tone: 'green' },
    IN_PROGRESS: { label: '작업 중', tone: 'blue' },
    COMPLETED: { label: '출차 대기', tone: 'amber' },
    GATE_OUT: { label: '출차 완료', tone: 'green' },
    CANCELED: { label: '반려', tone: 'red' },
    CANCELLED: { label: '반려', tone: 'red' },
  },
  process: {
    SUCCESS: { label: '성공', tone: 'green' },
    FAILED: { label: '실패', tone: 'red' },
    FAIL: { label: '실패', tone: 'red' },
    RECOGNITION_NEED_REVIEW: { label: '검토 필요', tone: 'amber' },
    NEED_REVIEW: { label: '검토 필요', tone: 'amber' },
    PENDING: { label: '대기', tone: 'amber' },
    WAITING: { label: '대기', tone: 'amber' },
  },
  user: {
    PENDING: { label: '승인 대기', tone: 'amber' },
    CARRIER_APPROVED: { label: '운송사 승인', tone: 'blue' },
    ACTIVE: { label: '활성', tone: 'green' },
    APPROVED: { label: '승인 완료', tone: 'green' },
    REJECTED: { label: '반려', tone: 'red' },
    WITHDRAWN: { label: '탈퇴', tone: 'gray' },
  },
  vehicle: {
    PENDING: { label: '승인 대기', tone: 'amber' },
    NORMAL: { label: '정상', tone: 'green' },
    ACTIVE: { label: '정상', tone: 'green' },
    APPROVED: { label: '승인 완료', tone: 'green' },
    REJECTED: { label: '승인 거절', tone: 'red' },
    승인대기: { label: '승인 대기', tone: 'amber' },
    정상: { label: '정상', tone: 'green' },
    승인거절: { label: '승인 거절', tone: 'red' },
  },
  inOut: {
    IN: { label: '입차', tone: 'blue' },
    OUT: { label: '출차', tone: 'red' },
  },
  boolean: {
    true: { label: '가능', tone: 'green' },
    false: { label: '불가', tone: 'red' },
  },
}

export const normalizeLabelValue = (value) => String(value ?? '').trim()

export const labelInfo = (type, value) => {
  if (value === true || value === false) {
    return LABELS.boolean[String(value)]
  }

  const normalized = normalizeLabelValue(value)
  const upper = normalized.toUpperCase()

  return (
    LABELS[type]?.[upper] ||
    LABELS[type]?.[normalized] ||
    { label: normalized || '-', tone: 'gray' }
  )
}

export const displayLabel = (type, value) => labelInfo(type, value).label

export const displayTone = (type, value) => labelInfo(type, value).tone

export const workStatusLabel = (value) => displayLabel('work', value)

export const processResultLabel = (value) => displayLabel('process', value)

export const inOutTypeLabel = (value) => displayLabel('inOut', value)

export const vehicleStatusLabel = (value) => displayLabel('vehicle', value)

export const userStatusLabel = (value) => displayLabel('user', value)

export const booleanLabel = (value) => {
  if (value === true || value === false) {
    return displayLabel('boolean', value)
  }

  return '-'
}
