<script setup>
import { computed, onMounted, ref } from 'vue'
import { useGateLogStore } from '@/stores/adminStore/gateLogStore'
import { useVehicleStore } from '@/stores/vehicleStore'
import { displayTone, inOutTypeLabel, processResultLabel } from '@/config/displayLabels'

const vehicleQuery = ref('')
const sortKey = ref('time')
const gateLogStore = useGateLogStore()
const vehicleStore = useVehicleStore()

const getPlateNumber = (vehicleId) => {
  const vehicle = vehicleStore.vehicles.find((item) => (item.vehicleId || item.vehicle_id) === vehicleId)
  return vehicle?.plateNumber || vehicle?.plate_number || vehicleId || '-'
}

const getLogVehiclePair = (log) => {
  const tractorVehicleId = log.tractorVehicleId || log.tractor_vehicle_id
  const trailerVehicleId = log.trailerVehicleId || log.trailer_vehicle_id
  const fallbackVehicleId = log.vehicleId || log.vehicle_id

  return {
    tractor: getPlateNumber(tractorVehicleId || fallbackVehicleId),
    trailer: getPlateNumber(trailerVehicleId || fallbackVehicleId),
  }
}

const logMatchesVehicleQuery = (log, query) => {
  const vehicles = getLogVehiclePair(log)
  return [vehicles.tractor, vehicles.trailer].some((plateNumber) =>
    String(plateNumber).toLowerCase().includes(query),
  )
}

const getInOutClass = (value) => (value === 'OUT' ? 'red' : 'blue')

const getProcessClass = (value) => displayTone('process', value)

const visibleLogs = computed(() => {
  const query = vehicleQuery.value.trim().toLowerCase()
  const filtered = query
    ? gateLogStore.gateLogs.filter((log) => logMatchesVehicleQuery(log, query))
    : [...gateLogStore.gateLogs]

  return filtered.sort((a, b) => {
    if (sortKey.value === 'gate') {
      return String(a.gateName || a.gate_name || '').localeCompare(String(b.gateName || b.gate_name || ''), 'ko')
    }
    return String(b.entryTime || b.entry_time || b.exitTime || b.exit_time || '').localeCompare(
      String(a.entryTime || a.entry_time || a.exitTime || a.exit_time || ''),
    )
  })
})

onMounted(() => {
  gateLogStore.loadGateLogs()
  vehicleStore.loadVehicles()
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>게이트 입출차 기록</h2>
        <div class="table-tools">
          <input v-model="vehicleQuery" type="search" placeholder="차량 번호" />
          <select v-model="sortKey" aria-label="정렬 기준">
            <option value="time">시간순</option>
            <option value="gate">게이트순</option>
          </select>
          <span class="status-pill">{{ visibleLogs.length }}건</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>로그 ID</th>
              <th>트랙터</th>
              <th>트레일러</th>
              <th>게이트 번호</th>
              <th>게이트명</th>
              <th>입차 시간</th>
              <th>출차 시간</th>
              <th>구분</th>
              <th>처리 결과</th>
              <th>관리자 확인</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in visibleLogs" :key="log.gateLogId || log.gate_log_id">
              <td>{{ log.gateLogId || log.gate_log_id }}</td>
              <td>{{ getLogVehiclePair(log).tractor }}</td>
              <td>{{ getLogVehiclePair(log).trailer }}</td>
              <td>{{ log.gateNumber || log.gate_number || '-' }}</td>
              <td>{{ log.gateName || log.gate_name || '-' }}</td>
              <td>{{ log.entryTime || log.entry_time || '-' }}</td>
              <td>{{ log.exitTime || log.exit_time || '-' }}</td>
              <td>
                <span class="status-pill" :class="getInOutClass(log.inOutType || log.in_out_type)">
                  {{ inOutTypeLabel(log.inOutType || log.in_out_type) }}
                </span>
              </td>
              <td>
                <span class="status-pill" :class="getProcessClass(log.processResult || log.process_result)">
                  {{ processResultLabel(log.processResult || log.process_result) }}
                </span>
              </td>
              <td>{{ log.managerCheck || log.manager_check || '-' }}</td>
            </tr>
            <tr v-if="visibleLogs.length === 0">
              <td colspan="10">게이트 로그 데이터가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.table-tools {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.table-tools input,
.table-tools select {
  min-height: 38px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid #c8d5e7;
  border-radius: 7px;
  padding: 0 10px;
  font-weight: 800;
}

.table-tools input {
  width: 180px;
}

@media (max-width: 760px) {
  .section-title {
    align-items: stretch;
    flex-direction: column;
  }

  .table-tools {
    justify-content: flex-start;
  }
}
</style>
