import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const source = resolve(root, '예지보전관련내용', 'predictive_maintenance_timeseries_synthetic.csv')
const target = resolve(root, 'frontend', 'public', 'data', 'antenna-maintenance-demo.json')

const lines = readFileSync(source, 'utf8').replace(/^\uFEFF/, '').trim().split(/\r?\n/)
const headers = lines.shift().split(',')
const index = Object.fromEntries(headers.map((header, column) => [header, column]))
const equipment = {}

for (const line of lines) {
  const row = line.split(',')
  const equipmentId = row[index.equipment_id]
  equipment[equipmentId] ??= []
  equipment[equipmentId].push([
    row[index.collected_at],
    Number(row[index.traffic_load]),
    Number(row[index.temperature_c]),
    Number(row[index.voltage_v]),
    Number(row[index.signal_strength_dbm]),
    Number(row[index.success_rate]),
    Number(row[index.response_time_ms]),
    Number(row[index.retry_count]),
    Number(row[index.disconnect_count]),
    Number(row[index.packet_loss_rate]),
    Number(row[index.error_count]),
    Number(row[index.days_since_maintenance]),
    Number(row[index.risk_score]),
    row[index.risk_level],
    Number(row[index.failure_event]),
    Number(row[index.maintenance_event]),
  ])
}

const payload = {
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
    'riskScore',
    'riskLevel',
    'failureEvent',
    'maintenanceEvent',
  ],
  equipment,
}

mkdirSync(dirname(target), { recursive: true })
writeFileSync(target, `${JSON.stringify(payload)}\n`, 'utf8')
console.log(`Wrote ${Object.keys(equipment).length} antennas and ${lines.length} records to ${target}`)
