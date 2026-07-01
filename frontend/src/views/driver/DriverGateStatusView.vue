<script setup>
import { computed } from 'vue'
import { useLogisticsData } from '@/composables/useLogisticsData'

const {
  gateLogs,
  getContainerNumber,
  getPlateNumber,
  getSectorByContainerId,
  workOrders,
} = useLogisticsData()

const driverOrders = computed(() => workOrders.value.filter((order) => [1, 3].includes(order.driver_id)))
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>최근 작업</h2>
        <span class="status-pill">{{ driverOrders.length }}건</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업 ID</th>
              <th>컨테이너</th>
              <th>작업 유형</th>
              <th>섹터</th>
              <th>예약 시간</th>
              <th>작업 상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in driverOrders" :key="order.work_order_id">
              <td>{{ order.work_order_id }}</td>
              <td>{{ getContainerNumber(order.container_id) }}</td>
              <td>{{ order.work_type }}</td>
              <td>{{ getSectorByContainerId(order.container_id)?.sector_name || '-' }}</td>
              <td>{{ order.reserved_time }}</td>
              <td><span class="status-pill">{{ order.work_status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>게이트 기록</h2>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>시간</th>
              <th>차량</th>
              <th>게이트</th>
              <th>구분</th>
              <th>처리 결과</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in gateLogs" :key="log.gate_log_id">
              <td>{{ log.entry_time || log.exit_time }}</td>
              <td>{{ getPlateNumber(log.vehicle_id) }}</td>
              <td>{{ log.gate_name }}</td>
              <td>{{ log.in_out_type }}</td>
              <td><span class="status-pill">{{ log.process_result }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
