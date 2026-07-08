<script setup>
import { computed, ref } from 'vue'
import { gateLogs } from '../../data/mockData'

const vehicleQuery = ref('')
const sortKey = ref('time')

const visibleLogs = computed(() => {
  const query = vehicleQuery.value.trim().toLowerCase()
  const filtered = query
    ? gateLogs.filter((log) => log.vehicleNo.toLowerCase().includes(query))
    : [...gateLogs]

  return filtered.sort((a, b) => {
    if (sortKey.value === 'gate') {
      return a.gate.localeCompare(b.gate, 'ko')
    }
    return a.time.localeCompare(b.time)
  })
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>게이트 로그</h2>
        <div class="table-tools">
          <input v-model="vehicleQuery" type="search" placeholder="차량 번호 검색" />
          <select v-model="sortKey" aria-label="정렬 기준">
            <option value="time">시간 순</option>
            <option value="gate">게이트 순</option>
          </select>
          <span class="status-pill">{{ visibleLogs.length }}건</span>
        </div>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>로그</th>
              <th>차량번호</th>
              <th>게이트</th>
              <th>입출차</th>
              <th>시간</th>
              <th>처리 결과</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in visibleLogs" :key="log.logNo">
              <td>{{ log.logNo }}</td>
              <td>{{ log.vehicleNo }}</td>
              <td>{{ log.gate }}</td>
              <td>{{ log.type }}</td>
              <td>{{ log.time }}</td>
              <td><span class="status-pill">{{ log.result }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.table-tools {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.table-tools input,
.table-tools select {
  min-height: 38px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid #c8d5e7;
  border-radius: 7px;
  padding: 0 10px;
  font-weight: 800;
}

.table-tools input {
  width: 180px;
}

@media (max-width: 760px) {
  .section-title {
    align-items: stretch;
    flex-direction: column;
  }

  .table-tools {
    justify-content: flex-start;
  }
}
</style>
