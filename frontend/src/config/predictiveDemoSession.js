import { reactive } from 'vue'

export const DEMO_EQUIPMENT_ID = 'DEMO-ANT'
export const DEMO_SOURCE_EQUIPMENT_ID = 'ANT-018'

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
  notificationRequests: [],
})

export const resetDemoNotifications = () => {
  predictiveDemoSession.notificationRequests = []
}

export const requestDemoNotification = async (eventType, occurredAt) => {
  const eventKey = `${eventType}-${occurredAt}`
  if (predictiveDemoSession.notificationRequests.some((item) => item.eventKey === eventKey)) return

  const request = {
    eventKey,
    equipmentId: DEMO_EQUIPMENT_ID,
    eventType,
    occurredAt,
    requestedAt: Date.now(),
    status: 'DEMO_REQUESTED',
  }
  predictiveDemoSession.notificationRequests.push(request)

  try {
    const response = await fetch('/api/predictive-maintenance/demo/notifications/kakao', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })
    const result = await response.json()
    request.status = result.status || (response.ok ? 'SENT' : 'FAILED')
  } catch {
    request.status = 'FAILED'
  }
}
