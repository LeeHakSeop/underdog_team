<script setup>
import { availableDrivers, containers } from '../../data/dbData'
</script>

<template>
  <div class="page-stack">
    <section class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>운송 요청 등록</h2>
        </div>
        <form class="form-grid">
          <div class="field">
            <label for="containerId">컨테이너</label>
            <select id="containerId">
              <option v-for="container in containers" :key="container.container_id" :value="container.container_id">
                {{ container.container_number }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="workType">작업 유형</label>
            <select id="workType">
              <option value="LOAD_OUT">반출 상차</option>
              <option value="UNLOAD_IN">반입 하차</option>
            </select>
          </div>
          <div class="field">
            <label for="reservedTime">예약 시간</label>
            <input id="reservedTime" type="datetime-local" value="2026-07-01T13:00" />
          </div>
          <div class="field">
            <label for="workStatus">작업 상태</label>
            <input id="workStatus" value="DISPATCH_WAITING" />
          </div>
          <button class="primary-button full request-button" type="button">운송 요청 등록</button>
        </form>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>출입 가능 기사</h2>
          <span class="status-pill green">{{ availableDrivers.length }}명</span>
        </div>
        <div class="match-list">
          <button v-for="driver in availableDrivers" :key="driver.driver_id" class="match-card" type="button">
            <span>
              <b>{{ driver.driver_name }}</b>
              <small>기사 ID {{ driver.driver_id }} / 운송사 ID {{ driver.carrier_id }}</small>
            </span>
            <strong>출입 가능 {{ driver.can_enter }}</strong>
          </button>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.full {
  grid-column: 1 / -1;
}

.request-button {
  min-height: 44px;
}

.match-list {
  display: grid;
  gap: 10px;
}

.match-card {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  text-align: left;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.match-card:hover {
  border-color: var(--blue-700);
}

.match-card span,
.match-card small {
  display: block;
}

.match-card b {
  font-size: 16px;
}

.match-card small {
  margin-top: 4px;
  color: var(--ink-500);
  font-weight: 700;
}

.match-card strong {
  color: var(--green-600);
}
</style>
