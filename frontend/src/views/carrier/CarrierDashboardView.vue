<script setup>
import { availableDrivers, workOrders } from '../../data/mockData'

const carrierOrders = workOrders.slice(0, 2)
</script>

<template>
  <div class="page-stack">
    <section class="grid-4">
      <article class="metric-card">
        <span>승인 대기</span>
        <strong>4</strong>
        <small>관리자 검토 중</small>
      </article>
      <article class="metric-card">
        <span>출입 가능 기사</span>
        <strong>7</strong>
        <small>등록 기사 기준</small>
      </article>
      <article class="metric-card">
        <span>오늘 반출</span>
        <strong>12</strong>
        <small>예약 기준</small>
      </article>
      <article class="metric-card">
        <span>반려</span>
        <strong>1</strong>
        <small>보류 컨테이너</small>
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
                <th>작업번호</th>
                <th>컨테이너</th>
                <th>섹터</th>
                <th>상태</th>
                <th>예약</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in carrierOrders" :key="order.orderNo">
                <td>{{ order.orderNo }}</td>
                <td>{{ order.containerNo }}</td>
                <td>{{ order.sectorCode }}</td>
                <td><span class="status-pill">{{ order.status }}</span></td>
                <td>{{ order.time }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>출입 가능 기사 후보</h2>
          <span class="status-pill green">can_enter=Y</span>
        </div>
        <div class="driver-list">
          <div v-for="driver in availableDrivers" :key="driver.vehicleNo" class="driver-row">
            <div>
              <b>{{ driver.name }}</b>
              <span>{{ driver.vehicleNo }} · {{ driver.distance }}</span>
            </div>
            <span class="status-pill green">{{ driver.status }}</span>
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
