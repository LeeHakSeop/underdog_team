<script setup>
import { computed } from 'vue'
import { useLogisticsData } from '@/composables/useLogisticsData'

const { workOrders, getCarrierName, getContainer, getContainerNumber, getDriverName, getPlateNumber, getSectorByContainerId } = useLogisticsData()
const carrierRequests = computed(() => {
  return workOrders.value.filter((order) => order.work_status === 'DISPATCH_WAITING')
})

const acceptedDriverTasks = computed(() => {
  return workOrders.value.filter((order) => ['APPROVED', 'IN_PROGRESS', 'WORKING'].includes(order.work_status))
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>운송사 작업 요청 승인관리</h2>
        <span class="status-pill amber">배차 대기 {{ carrierRequests.length }}건</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업번호</th>
              <th>운송사</th>
              <th>차량번호</th>
              <th>기사</th>
              <th>컨테이너</th>
              <th>작업 유형</th>
              <th>예약</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in carrierRequests" :key="order.work_order_id">
              <td>{{ order.work_order_id }}</td>
              <td>{{ getCarrierName(order.carrier_id) }}</td>
              <td>{{ getPlateNumber(order.vehicle_id) }}</td>
              <td>{{ getDriverName(order.driver_id) }}</td>
              <td>{{ getContainerNumber(order.container_id) }}</td>
              <td>{{ order.work_type }}</td>
              <td>{{ order.reserved_time }}</td>
              <td><span class="status-pill amber">{{ order.work_status }}</span></td>
              <td><button class="ghost-button" type="button">요청 확인</button></td>
            </tr>
            <tr v-if="carrierRequests.length === 0">
              <td colspan="9">배차 대기 작업이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>기사 승낙 작업 관리</h2>
        <span class="status-pill green">섹터 배치 대상 {{ acceptedDriverTasks.length }}건</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업번호</th>
              <th>화물 종류</th>
              <th>컨테이너</th>
              <th>차량번호</th>
              <th>기사</th>
              <th>자동 배정 섹터</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in acceptedDriverTasks" :key="order.work_order_id">
              <td>{{ order.work_order_id }}</td>
              <td>{{ getContainer(order.container_id)?.shipping_line || '-' }}</td>
              <td>{{ getContainerNumber(order.container_id) }}</td>
              <td>{{ getPlateNumber(order.vehicle_id) }}</td>
              <td>{{ getDriverName(order.driver_id) }}</td>
              <td>{{ getSectorByContainerId(order.container_id)?.sector_name || '-' }}</td>
              <td><span class="status-pill green">{{ order.work_status }}</span></td>
              <td><button class="primary-button" type="button">섹터 배치 승인</button></td>
            </tr>
            <tr v-if="acceptedDriverTasks.length === 0">
              <td colspan="8">섹터 배치 대상 작업이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
