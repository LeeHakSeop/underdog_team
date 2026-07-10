<script setup>
import { computed } from 'vue'
import {
  getCarrierName,
  getContainerNumber,
  getDriverName,
  getPlateNumber,
  getSectorByContainerId,
  workOrders,
} from '../../data/dbData'

const pendingOrders = computed(() => {
  return workOrders.filter((order) => order.work_status === 'DISPATCH_WAITING')
})

const acceptedOrders = computed(() => {
  return workOrders.filter((order) => ['DRIVER_ACCEPTED', 'APPROVED'].includes(order.work_status))
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>운송 요청 승인</h2>
        <span class="status-pill amber">승인 대기 {{ pendingOrders.length }}건</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업 ID</th>
              <th>운송사</th>
              <th>차량</th>
              <th>기사</th>
              <th>컨테이너</th>
              <th>작업 유형</th>
              <th>예약 시간</th>
              <th>작업 상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in pendingOrders" :key="order.work_order_id">
              <td>{{ order.work_order_id }}</td>
              <td>{{ getCarrierName(order.carrier_id) }}</td>
              <td>{{ getPlateNumber(order.vehicle_id) }}</td>
              <td>{{ getDriverName(order.driver_id) }}</td>
              <td>{{ getContainerNumber(order.container_id) }}</td>
              <td>{{ order.work_type }}</td>
              <td>{{ order.reserved_time }}</td>
              <td><span class="status-pill amber">{{ order.work_status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>배정된 작업</h2>
        <span class="status-pill green">{{ acceptedOrders.length }}건</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업 ID</th>
              <th>컨테이너</th>
              <th>차량</th>
              <th>기사</th>
              <th>섹터</th>
              <th>작업 상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in acceptedOrders" :key="order.work_order_id">
              <td>{{ order.work_order_id }}</td>
              <td>{{ getContainerNumber(order.container_id) }}</td>
              <td>{{ getPlateNumber(order.vehicle_id) }}</td>
              <td>{{ getDriverName(order.driver_id) }}</td>
              <td>{{ getSectorByContainerId(order.container_id)?.sector_name || '-' }}</td>
              <td><span class="status-pill green">{{ order.work_status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
