import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { basename, dirname, resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const defaultBundle = resolve(
  root,
  '예지보전관련내용',
  'pm_visualization_bundle_v3',
  'pm_visualization_bundle_v3',
)
const bundle = process.argv[2] ? resolve(root, process.argv[2]) : defaultBundle
const source = resolve(bundle, '01_dashboard_timeseries.csv')
const target = resolve(root, 'frontend', 'public', 'data', 'antenna-maintenance-demo.json')
const metadataTarget = resolve(root, 'frontend', 'public', 'data', 'predictive-maintenance-metadata.json')
const metadataSourceTarget = resolve(root, 'frontend', 'src', 'data', 'predictive-maintenance-metadata.json')
const operationalCsvTarget = resolve(root, 'DB', 'data', '01_dashboard_timeseries.csv')

const mapEquipment = (sourceCode) => {
  const match = /^ANT-(\d{3})$/.exec(sourceCode)
  if (!match) return { code: sourceCode, type: 'port_equipment', location: '' }
  const number = Number(match[1])
  if (number <= 4) {
    const index = number
    return { code: `GAT-${String(index).padStart(3, '0')}`, type: 'gate_recognition', location: `GATE-${String(index).padStart(2, '0')}` }
  }
  if (number <= 12) {
    const index = number - 4
    return { code: `QC-${String(index).padStart(3, '0')}`, type: 'quay_crane', location: `QUAY-${String(index).padStart(2, '0')}` }
  }
  if (number <= 20) {
    const index = number - 12
    return { code: `TC-${String(index).padStart(3, '0')}`, type: 'transfer_crane', location: `YARD-TC-${String(index).padStart(2, '0')}` }
  }
  const index = number - 20
  return { code: `YT-${String(index).padStart(3, '0')}`, type: 'yard_tractor', location: `YARD-YT-${String(index).padStart(2, '0')}` }
}

const readCsv = (path) => {
  const lines = readFileSync(path, 'utf8').replace(/^\uFEFF/, '').trim().split(/\r?\n/)
  const headers = lines.shift().split(',')
  return lines.map((line) => {
    const values = line.split(',')
    return Object.fromEntries(headers.map((header, index) => [header, values[index]]))
  })
}

const toBoolean = (value) => value === 'True' || value === 'true' || value === '1'
const toNullableNumber = (value) => (value === '' || value === undefined ? null : Number(value))

const equipment = {}
const sourceDashboardRows = readCsv(source)
const dashboardRows = sourceDashboardRows.map((row) => {
  const mapped = mapEquipment(row.equipment_id)
  return {
    ...row,
    equipment_id: mapped.code,
    equipment_type: mapped.type,
    location_id: mapped.location,
  }
})

for (const row of dashboardRows) {
  equipment[row.equipment_id] ??= []
  equipment[row.equipment_id].push([
    row.collected_at,
    Number(row.traffic_load),
    Number(row.temperature_c),
    Number(row.voltage_v),
    Number(row.signal_strength_dbm),
    Number(row.success_rate),
    Number(row.response_time_ms),
    Number(row.retry_count),
    Number(row.disconnect_count),
    Number(row.packet_loss_rate),
    Number(row.error_count),
    Number(row.days_since_maintenance),
    Number(row.reference_failure_event),
    Number(row.maintenance_event),
    Number(row.current_fault_probability),
    Number(row.anomaly_count),
    row.abnormal_sensors,
    toBoolean(row.current_failure),
    row.operational_state,
    toBoolean(row.precursor_entry_condition),
    toBoolean(row.state_changed),
    row.operational_state_ko,
    Number(row.state_level),
    toBoolean(row.needs_attention),
  ])
}

const sensorLimits = {}
for (const row of readCsv(resolve(bundle, '04_sensor_limits_by_equipment.csv'))) {
  const equipmentId = mapEquipment(row.equipment_id).code
  sensorLimits[equipmentId] ??= {}
  sensorLimits[equipmentId][row.sensor] = {
    direction: row.direction,
    limit: Number(row.limit),
  }
}

const sensorMetadata = Object.fromEntries(
  readCsv(resolve(bundle, '06_sensor_metadata.csv')).map((row) => [
    row.column,
    {
      label: row.label_ko,
      unit: row.unit,
      direction: row.abnormal_direction,
    },
  ]),
)

const displayMetadata = {
  success_rate: { label: '동작 성공률', unit: '%' },
  response_time_ms: { label: '제어 응답 시간', unit: 'ms' },
  packet_loss_rate: { label: '제어 데이터 손실률', unit: '%' },
  traffic_load: { label: '장비 작업 부하', unit: '' },
  temperature_c: { label: '제어장치 온도', unit: '℃' },
  voltage_v: { label: '제어회로 전압', unit: 'V' },
  signal_strength_dbm: { label: '센서 신호 품질', unit: '점' },
  retry_count: { label: '재동작 횟수', unit: '회' },
  disconnect_count: { label: '제어 통신 중단', unit: '회' },
  error_count: { label: '장비 오류 횟수', unit: '회' },
}
for (const [sensor, display] of Object.entries(displayMetadata)) {
  if (sensorMetadata[sensor]) Object.assign(sensorMetadata[sensor], display)
}

const precursorLinks = readCsv(resolve(bundle, '03_failure_precursor_links.csv')).map((row) => ({
  equipmentId: mapEquipment(row.equipment_id).code,
  failureAt: row.failure_time,
  precursorStartedAt: row.latest_precursor_start || null,
  leadHours: toNullableNumber(row.lead_hours),
}))

const alertEvents = dashboardRows
  .filter(
    (row) =>
      toBoolean(row.state_changed) && row.operational_state === 'failure_expected',
  )
  .map((row) => ({
    id: `${row.equipment_id}-${row.collected_at}`,
    equipmentId: row.equipment_id,
    occurredAt: row.collected_at,
    type: 'PREDICTIVE_FAILURE_EXPECTED',
    channel: 'KAKAO',
    deliveryStatus: 'SUPPRESSED_PAST',
    anomalyCount: Number(row.anomaly_count),
    abnormalSensors: row.abnormal_sensors ? row.abnormal_sensors.split('|') : [],
    message: `[항만 운영장비 고장 예상] ${row.equipment_id}에서 고장 전조가 확인되었습니다. 점검이 필요합니다.`,
  }))

const historicalNotificationEvents = [
  ...alertEvents.map((event) => ({
    ...event,
    eventType: 'FAILURE_EXPECTED',
    suppressedReason: 'HISTORICAL_REPLAY',
  })),
  ...dashboardRows
    .filter(
      (row) =>
        toBoolean(row.state_changed) && row.operational_state === 'failure',
    )
    .map((row) => ({
      id: `${row.equipment_id}-${row.collected_at}-failure`,
      equipmentId: row.equipment_id,
      occurredAt: row.collected_at,
      eventType: 'FAILURE',
      channel: 'KAKAO',
      deliveryStatus: 'SUPPRESSED_PAST',
      suppressedReason: 'HISTORICAL_REPLAY',
    })),
]

const policy = JSON.parse(readFileSync(resolve(bundle, 'final_operational_policy.json'), 'utf8'))
const currentFaultModel = JSON.parse(
  readFileSync(resolve(bundle, 'current_fault_model_summary.json'), 'utf8'),
)

const payload = {
  datasetVersion: '항만 운영장비 V3 운영정책',
  sourceFile: basename(source),
  generatedFrom: 'pm_visualization_bundle_v3',
  policy,
  currentFaultModel,
  sensorLimits,
  sensorMetadata,
  precursorLinks,
  alertEvents,
  historicalNotificationEvents,
  columns: [
    'collectedAt',
    'trafficLoad',
    'temperatureC',
    'voltageV',
    'signalStrengthDbm',
    'successRate',
    'responseTimeMs',
    'retryCount',
    'disconnectCount',
    'packetLossRate',
    'errorCount',
    'daysSinceMaintenance',
    'failureEvent',
    'maintenanceEvent',
    'currentFaultProbability',
    'anomalyCount',
    'abnormalSensors',
    'currentFailure',
    'operationalState',
    'precursorEntryCondition',
    'stateChanged',
    'operationalStateKo',
    'stateLevel',
    'needsAttention',
  ],
  equipment,
}

mkdirSync(dirname(target), { recursive: true })
mkdirSync(dirname(metadataSourceTarget), { recursive: true })
mkdirSync(dirname(operationalCsvTarget), { recursive: true })
writeFileSync(target, `${JSON.stringify(payload)}\n`, 'utf8')
const metadataPayload = `${JSON.stringify({
  datasetVersion: payload.datasetVersion,
  sensorLimits: payload.sensorLimits,
  sensorMetadata: payload.sensorMetadata,
  policy: payload.policy,
  currentFaultModel: payload.currentFaultModel,
})}\n`
writeFileSync(metadataTarget, metadataPayload, 'utf8')
writeFileSync(metadataSourceTarget, metadataPayload, 'utf8')

const csvHeaders = Object.keys(dashboardRows[0])
writeFileSync(
  operationalCsvTarget,
  `${csvHeaders.join(',')}\n${dashboardRows.map((row) => csvHeaders.map((header) => row[header] ?? '').join(',')).join('\n')}\n`,
  'utf8',
)
console.log(
  `Wrote ${Object.keys(equipment).length} port equipment units and ${dashboardRows.length} operational-policy records to ${target}`,
)
