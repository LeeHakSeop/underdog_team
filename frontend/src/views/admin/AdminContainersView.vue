<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useContainerStore } from '@/stores/adminStore/containerStore'
import { useYardSectorStore } from '@/stores/adminStore/yardSectorStore'

const containerQuery = ref('')
const containerStore = useContainerStore()
const yardSectorStore = useYardSectorStore()
const { containers } = storeToRefs(containerStore)
const { yardSectors } = storeToRefs(yardSectorStore)

onMounted(() => {
  containerStore.loadContainers()
  yardSectorStore.loadYardSectors()
})

const getSectorName = (sectorId) => {
  return yardSectors.value.find((sector) => sector.sectorId === sectorId)?.sectorName || '-'
}

const visibleContainers = computed(() => {
  const query = containerQuery.value.trim().toLowerCase()
  if (!query) return containers.value
  return containers.value.filter((container) => container.containerNumber.toLowerCase().includes(query))
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
            <tr v-for="container in visibleContainers" :key="container.containerId">
              <td>{{ container.containerId }}</td>
              <td>{{ container.containerNumber }}</td>
              <td>{{ container.containerSize }}</td>
              <td>{{ container.shippingLine || '-' }}</td>
              <td>{{ container.containerLocation }}</td>
              <td>{{ getSectorName(container.sectorId) }}</td>
              <td><span class="status-pill" :class="{ green: container.canExit }">{{ container.canExit }}</span></td>
              <td>{{ container.sealNumber || '-' }}</td>
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
