import { reactive } from 'vue'
import { request } from '@/api/apiClient'

export const DEMO_EQUIPMENT_ID = 'DEMO-TC'
export const DEMO_SOURCE_EQUIPMENT_ID = 'TC-006'

export const predictiveDemoSession = reactive({
  initialized: false,
  selectedEquipmentId: DEMO_EQUIPMENT_ID,
  observationTime: 0,
  alertAt: 0,
  failureAt: 0,
  maintenanceAt: 0,
  anomalyCountAtAlert: 0,
  anomalyCountAtFailure: 0,
  maintenanceState: '정상',
  kakaoNotificationsEnabled: false,
  notificationRequests: [],
})

export const resetDemoNotifications = () => {
  predictiveDemoSession.notificationRequests = []
}

export const setKakaoNotificationsEnabled = (enabled) => {
  predictiveDemoSession.kakaoNotificationsEnabled = Boolean(enabled)
}

export const fetchKakaoRuntimeStatus = async () => {
  return request('/api/predictive-maintenance/demo/notifications/kakao/config')
}

export const beginKakaoOAuth = async () => {
  return request('/api/predictive-maintenance/demo/notifications/kakao/oauth/authorize')
}

export const clearKakaoRuntime = async (userId) => {
  return request(`/api/predictive-maintenance/demo/notifications/kakao/config/${encodeURIComponent(userId)}`, {
    method: 'DELETE',
  })
}

export const requestDemoNotification = async (eventType, occurredAt) => {
  if (!predictiveDemoSession.kakaoNotificationsEnabled) {
    return { status: 'DISABLED' }
  }

  const eventKey = `${eventType}-${occurredAt}`
  if (predictiveDemoSession.notificationRequests.some((item) => item.eventKey === eventKey)) return

  const notificationRequest = {
    eventKey,
    equipmentId: DEMO_EQUIPMENT_ID,
    eventType,
    occurredAt,
    requestedAt: Date.now(),
    status: 'DEMO_REQUESTED',
  }
  predictiveDemoSession.notificationRequests.push(notificationRequest)

  try {
    const result = await request('/api/predictive-maintenance/demo/notifications/kakao', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(notificationRequest),
    })
    notificationRequest.status = result.status || 'SENT'
  } catch {
    notificationRequest.status = 'FAILED'
  }
}
