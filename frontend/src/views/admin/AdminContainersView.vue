<script setup>
import { computed, ref } from 'vue'
import { containers, getSectorName } from '../../data/dbData'

const containerQuery = ref('')

const visibleContainers = computed(() => {
  const query = containerQuery.value.trim().toLowerCase()
  if (!query) return containers
  return containers.filter((container) => container.container_number.toLowerCase().includes(query))
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>컨테이너 조회</h2>
        <div class="table-tools">
          <input v-model="containerQuery" type="search" placeholder="컨테이너 번호" />
          <span class="status-pill">{{ visibleContainers.length }}건</span>
        </div>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>컨테이너 ID</th>
              <th>컨테이너 번호</th>
              <th>규격</th>
              <th>유형</th>
              <th>현재 위치</th>
              <th>섹터</th>
              <th>반출 가능</th>
              <th>보류 여부</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="container in visibleContainers" :key="container.container_id">
              <td>{{ container.container_id }}</td>
              <td>{{ container.container_number }}</td>
              <td>{{ container.container_size }}</td>
              <td>{{ container.container_type }}</td>
              <td>{{ container.current_location }}</td>
              <td>{{ getSectorName(container.sector_id) }}</td>
              <td><span class="status-pill" :class="{ green: container.can_exit }">{{ container.can_exit }}</span></td>
              <td><span class="status-pill" :class="{ red: container.on_hold }">{{ container.on_hold }}</span></td>
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

.table-tools input {
  min-height: 38px;
  width: 220px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid #c8d5e7;
  border-radius: 7px;
  padding: 0 10px;
  font-weight: 800;
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
