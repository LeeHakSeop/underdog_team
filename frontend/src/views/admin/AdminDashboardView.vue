<script setup>
import { computed, ref } from 'vue'
import SectorMap from '../../components/yard/SectorMap.vue'
import { useLogisticsData } from '@/composables/useLogisticsData'

const {
  getCarrierName,
  getContainerNumber,
  getDriverName,
  getPlateNumber,
  getSectorByContainerId,
  operationStats,
  workOrders,
} = useLogisticsData()

const selectedSectorCode = ref('B-07')

const sectorOrders = computed(() => {
  return workOrders.value.filter((order) => {
    return getSectorByContainerId(order.container_id)?.sector_name === selectedSectorCode.value
  })
})
</script>

<template>
  <div class="page-stack">
    <section class="grid-4">
      <article v-for="stat in operationStats" :key="stat.label" class="metric-card">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <small>{{ stat.hint }}</small>
      </article>
    </section>

    <section class="grid-2 admin-grid">
      <article class="panel">
        <SectorMap :selected-code="selectedSectorCode" @select="selectedSectorCode = $event" />
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>{{ selectedSectorCode }} 진행 작업</h2>
          <span class="status-pill">{{ sectorOrders.length }}건</span>
        </div>

        <div v-if="sectorOrders.length" class="sector-work-list">
          <article v-for="order in sectorOrders" :key="order.work_order_id" class="sector-work-card">
            <div>
              <b>작업 ID: {{ order.work_order_id }}</b>
              <span>{{ getPlateNumber(order.vehicle_id) }} / {{ getDriverName(order.driver_id) }}</span>
            </div>
            <dl>
              <div>
                <dt>컨테이너</dt>
                <dd>{{ getContainerNumber(order.container_id) }}</dd>
              </div>
              <div>
                <dt>작업 유형</dt>
                <dd>{{ order.work_type }}</dd>
              </div>
              <div>
                <dt>예약 시간</dt>
                <dd>{{ order.reserved_time }}</dd>
              </div>
              <div>
                <dt>작업 상태</dt>
                <dd><span class="status-pill">{{ order.work_status }}</span></dd>
              </div>
            </dl>
          </article>
        </div>

        <div v-else class="empty-state">선택한 섹터에 진행 중인 작업이 없습니다.</div>
      </article>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>작업 현황</h2>
        <RouterLink class="ghost-button" to="/admin/work-orders">상세 보기</RouterLink>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업 ID</th>
              <th>운송사</th>
              <th>차량</th>
              <th>기사</th>
              <th>컨테이너</th>
              <th>작업 상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in workOrders" :key="order.work_order_id">
              <td>{{ order.work_order_id }}</td>
              <td>{{ getCarrierName(order.carrier_id) }}</td>
              <td>{{ getPlateNumber(order.vehicle_id) }}</td>
              <td>{{ getDriverName(order.driver_id) }}</td>
              <td>{{ getContainerNumber(order.container_id) }}</td>
              <td><span class="status-pill">{{ order.work_status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-grid {
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.9fr);
}

.sector-work-list {
  display: grid;
  gap: 12px;
}

.sector-work-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.sector-work-card b,
.sector-work-card span {
  display: block;
}

.sector-work-card span {
  margin-top: 4px;
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}

.sector-work-card dl {
  display: grid;
  gap: 8px;
  margin: 0;
}

.sector-work-card dl div {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 10px;
}

.sector-work-card dt {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.sector-work-card dd {
  margin: 0;
  font-weight: 800;
}

.empty-state {
  padding: 24px;
  color: var(--ink-500);
  background: #f6f9fd;
  border: 1px dashed var(--line);
  border-radius: 8px;
  font-weight: 800;
}

@media (max-width: 1100px) {
  .admin-grid {
    grid-template-columns: 1fr;
  }
}
</style>
