<script setup>
import {
  getContainerNumber,
  getDriverName,
  getPlateNumber,
  getSectorByContainerId,
  workOrders,
} from '../../data/dbData'
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>승인 현황</h2>
        <span class="status-pill">{{ workOrders.length }}건</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업 ID</th>
              <th>작업 유형</th>
              <th>기사</th>
              <th>차량</th>
              <th>컨테이너</th>
              <th>섹터</th>
              <th>작업 상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in workOrders" :key="order.work_order_id">
              <td>{{ order.work_order_id }}</td>
              <td>{{ order.work_type }}</td>
              <td>{{ getDriverName(order.driver_id) }}</td>
              <td>{{ getPlateNumber(order.vehicle_id) }}</td>
              <td>{{ getContainerNumber(order.container_id) }}</td>
              <td>{{ getSectorByContainerId(order.container_id)?.sector_name || '-' }}</td>
              <td>
                <span class="status-pill" :class="{ amber: !order.is_approved, green: order.is_approved }">
                  {{ order.work_status }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
