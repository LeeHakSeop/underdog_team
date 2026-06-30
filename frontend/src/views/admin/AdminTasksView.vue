<script setup>
import { computed } from 'vue'
import { workOrders } from '../../data/mockData'

const carrierRequests = computed(() => {
  return workOrders.filter((order) => order.status === '배차 대기')
})

const acceptedDriverTasks = computed(() => {
  return workOrders.filter((order) => ['기사 승낙', '상차 진행'].includes(order.status))
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
            <tr v-for="order in carrierRequests" :key="order.orderNo">
              <td>{{ order.orderNo }}</td>
              <td>{{ order.carrierName }}</td>
              <td>{{ order.vehicleNo }}</td>
              <td>{{ order.driverName }}</td>
              <td>{{ order.containerNo }}</td>
              <td>{{ order.workType }}</td>
              <td>{{ order.time }}</td>
              <td><span class="status-pill amber">{{ order.status }}</span></td>
              <td><button class="ghost-button" type="button">요청 확인</button></td>
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
            <tr v-for="order in acceptedDriverTasks" :key="order.orderNo">
              <td>{{ order.orderNo }}</td>
              <td>{{ order.cargoType }}</td>
              <td>{{ order.containerNo }}</td>
              <td>{{ order.vehicleNo }}</td>
              <td>{{ order.driverName }}</td>
              <td>{{ order.sectorCode }}</td>
              <td><span class="status-pill green">{{ order.status }}</span></td>
              <td><button class="primary-button" type="button">섹터 배치 승인</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
