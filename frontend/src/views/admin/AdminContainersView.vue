<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useContainerStore } from '@/stores/adminStore/containerStore'

const containerQuery = ref('')
const containerStore = useContainerStore()
const { containers, loading, error } = storeToRefs(containerStore)

const visibleContainers = computed(() => {
  const query = containerQuery.value.trim().toLowerCase()
  if (!query) return containers.value
  return containers.value.filter((container) =>
    String(container.containerNumber || container.container_number || '').toLowerCase().includes(query),
  )
})

onMounted(() => {
  containerStore.loadContainers()
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

      <div v-if="loading" class="empty-box">
        컨테이너 목록을 불러오는 중입니다.
      </div>
      <div v-else-if="error" class="empty-box warning">
        {{ error }}
      </div>

      <div class="table-wrap">
        <table v-if="!loading && !error" class="data-table">
          <thead>
            <tr>
              <th>컨테이너 ID</th>
              <th>컨테이너 번호</th>
              <th>규격</th>
              <th>선사</th>
              <th>현재 위치</th>
              <th>야드 위치</th>
              <th>반출 가능</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="container in visibleContainers" :key="container.containerId || container.container_id">
              <td>{{ container.containerId || container.container_id }}</td>
              <td>{{ container.containerNumber || container.container_number }}</td>
              <td>{{ container.containerSize || container.container_size || '-' }}</td>
              <td>{{ container.shippingLine || container.shipping_line || '-' }}</td>
              <td>{{ container.containerLocation || container.container_location || '-' }}</td>
              <td>
                {{ container.block || '-' }}-{{ container.bay || '-' }}-{{ container.rowNo || container.row_no || '-' }}
              </td>
              <td>
                <span class="status-pill" :class="{ red: !(container.canExit ?? container.can_exit) }">
                  {{ container.canExit ?? container.can_exit ? '가능' : '보류' }}
                </span>
              </td>
            </tr>
            <tr v-if="visibleContainers.length === 0">
              <td colspan="7">컨테이너 데이터가 없습니다.</td>
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

.empty-box {
  padding: 18px;
  color: var(--ink-500);
  background: #f8fbfe;
  border: 1px solid var(--line);
  font-weight: 800;
}

.empty-box.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
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
