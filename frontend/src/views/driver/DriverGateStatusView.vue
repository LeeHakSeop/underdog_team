<script setup>
import { gateLogs, workOrders } from '../../data/mockData'

const driverOrders = workOrders.filter((order) => order.driverName === '김도현' || order.driverName === '박민재')
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
              <th>작업번호</th>
              <th>컨테이너</th>
              <th>작업 유형</th>
              <th>섹터</th>
              <th>예약</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in driverOrders" :key="order.orderNo">
              <td>{{ order.orderNo }}</td>
              <td>{{ order.containerNo }}</td>
              <td>{{ order.workType }}</td>
              <td>{{ order.sectorCode }}</td>
              <td>{{ order.time }}</td>
              <td><span class="status-pill">{{ order.status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>기록 로그</h2>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>시간</th>
              <th>차량번호</th>
              <th>게이트</th>
              <th>구분</th>
              <th>결과</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in gateLogs" :key="log.logNo">
              <td>{{ log.time }}</td>
              <td>{{ log.vehicleNo }}</td>
              <td>{{ log.gate }}</td>
              <td>{{ log.type }}</td>
              <td><span class="status-pill">{{ log.result }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
