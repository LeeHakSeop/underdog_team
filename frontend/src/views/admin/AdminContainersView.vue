<script setup>
import { computed, ref } from 'vue'
import { containers } from '../../data/mockData'

const containerQuery = ref('')

const visibleContainers = computed(() => {
  const query = containerQuery.value.trim().toLowerCase()
  if (!query) return containers
  return containers.filter((container) => container.containerNo.toLowerCase().includes(query))
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>컨테이너 목록</h2>
        <div class="table-tools">
          <input v-model="containerQuery" type="search" placeholder="컨테이너 번호 검색" />
          <span class="status-pill">{{ visibleContainers.length }}건</span>
        </div>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>컨테이너 번호</th>
              <th>규격</th>
              <th>유형</th>
              <th>현재 위치</th>
              <th>섹터</th>
              <th>반출</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="container in visibleContainers" :key="container.containerNo">
              <td>{{ container.containerNo }}</td>
              <td>{{ container.sizeType }}</td>
              <td>{{ container.type }}</td>
              <td>{{ container.location }}</td>
              <td>{{ container.sectorCode }}</td>
              <td><span class="status-pill" :class="{ red: container.release === '보류' }">{{ container.release }}</span></td>
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
