<script setup>
import {
  availableDrivers,
  containers,
  getContainerNumber,
  getSectorByContainerId,
  workOrders,
} from '../../data/dbData'

const carrierOrders = workOrders.slice(0, 2)
</script>

<template>
  <div class="page-stack">
    <section class="grid-4">
      <article class="metric-card">
        <span>승인 대기 작업</span>
        <strong>{{ workOrders.filter((order) => !order.is_approved).length }}</strong>
        <small>관리자 승인 필요</small>
      </article>
      <article class="metric-card">
        <span>출입 가능 기사</span>
        <strong>{{ availableDrivers.length }}</strong>
        <small>기사 출입 권한</small>
      </article>
      <article class="metric-card">
        <span>컨테이너</span>
        <strong>{{ containers.length }}</strong>
        <small>컨테이너 기준</small>
      </article>
      <article class="metric-card">
        <span>보류 컨테이너</span>
        <strong>{{ containers.filter((container) => container.on_hold).length }}</strong>
        <small>반출 보류</small>
      </article>
    </section>

    <section class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>최근 운송 요청</h2>
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>작업 ID</th>
                <th>컨테이너</th>
                <th>섹터</th>
                <th>작업 상태</th>
                <th>예약 시간</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in carrierOrders" :key="order.work_order_id">
                <td>{{ order.work_order_id }}</td>
                <td>{{ getContainerNumber(order.container_id) }}</td>
                <td>{{ getSectorByContainerId(order.container_id)?.sector_name || '-' }}</td>
                <td><span class="status-pill">{{ order.work_status }}</span></td>
                <td>{{ order.reserved_time }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>출입 가능 기사</h2>
          <span class="status-pill green">출입 가능</span>
        </div>
        <div class="driver-list">
          <div v-for="driver in availableDrivers" :key="driver.driver_id" class="driver-row">
            <div>
              <b>{{ driver.driver_name }}</b>
              <span>연락처 {{ driver.driver_contact }} / 운송사 ID {{ driver.carrier_id }}</span>
            </div>
            <span class="status-pill green">{{ driver.can_enter }}</span>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.driver-list {
  display: grid;
  gap: 10px;
}

.driver-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.driver-row b,
.driver-row span {
  display: block;
}

.driver-row b {
  margin-bottom: 4px;
}

.driver-row div span {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}
</style>
