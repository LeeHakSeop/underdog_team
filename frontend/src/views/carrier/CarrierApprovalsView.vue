<script setup>
import { workOrders } from '../../data/mockData'
</script>

<template>
  <div class="page-stack">

    <section class="panel">
      <div class="section-title">
        <h2>요청 목록</h2>
        <span class="status-pill">{{ workOrders.length }}건</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업번호</th>
              <th>작업 유형</th>
              <th>기사</th>
              <th>차량번호</th>
              <th>컨테이너</th>
              <th>섹터</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in workOrders" :key="order.orderNo">
              <td>{{ order.orderNo }}</td>
              <td>{{ order.workType }}</td>
              <td>{{ order.driverName }}</td>
              <td>{{ order.vehicleNo }}</td>
              <td>{{ order.containerNo }}</td>
              <td>{{ order.sectorCode }}</td>
              <td>
                <span class="status-pill" :class="{ amber: order.status === '승인 대기', green: order.status === '상차 완료' }">
                  {{ order.status }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
