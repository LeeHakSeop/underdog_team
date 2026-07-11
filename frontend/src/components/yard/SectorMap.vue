<script setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useDashboardStore } from '@/stores/adminStore/dashboardStore'

const props = defineProps({
  selectedCode: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['select'])

const dashboardStore = useDashboardStore()
const { dashboard } = storeToRefs(dashboardStore)

const mapPositions = [
  { x: 19, y: 27 },
  { x: 38, y: 27 },
  { x: 58, y: 43 },
  { x: 78, y: 43 },
  { x: 30, y: 69 },
  { x: 68, y: 69 },
]

const sectorList = computed(() => dashboard.value?.sectorList || [])

const sectors = computed(() =>
  sectorList.value.map((sector, index) => ({
    ...sector,
    code: sector.sectorName,
    count: sector.waitingVehicleCount || 0,
    statusText: sector.sectorStatus || '-',
    className:
      sector.sectorStatus === '혼잡'
        ? 'congested'
        : sector.sectorStatus === '작업중'
          ? 'working'
          : 'available',
    x: mapPositions[index % mapPositions.length].x,
    y: mapPositions[index % mapPositions.length].y,
  })),
)

const selectedSector = computed(() => {
  return sectors.value.find((sector) => sector.code === props.selectedCode) || sectors.value[0]
})

onMounted(() => {
  dashboardStore.loadDashboard()
})
</script>

<template>
  <div class="sector-map">
    <div class="map-header">
      <div>
        <b>야드 섹터 안내</b>
        <span>섹터별 대기 차량 현황을 확인합니다.</span>
      </div>
      <span class="status-pill green">{{ selectedSector?.code || '-' }}</span>
    </div>

    <div class="yard-canvas">
      <div class="gate-marker">GATE</div>
      <button
        v-for="sector in sectors"
        :key="sector.sectorId"
        class="sector-node"
        :class="[sector.className, { selected: sector.code === selectedSector?.code }]"
        type="button"
        :style="{ left: `${sector.x}%`, top: `${sector.y}%` }"
        @click="emit('select', sector.code)"
      >
        <strong>{{ sector.code }}</strong>
        <small>대기 {{ sector.count }}대</small>
      </button>
    </div>

    <div class="map-footer">
      <span>선택 섹터: {{ selectedSector?.code || '-' }}</span>
      <span>{{ selectedSector?.statusText || '-' }}</span>
      <span>대기 차량: {{ selectedSector?.count || 0 }}대</span>
    </div>
  </div>
</template>

<style scoped>
.sector-map {
  display: grid;
  gap: 8px;
}

.map-header,
.map-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.map-header b {
  display: block;
  font-size: 14px;
}

.map-header span,
.map-footer span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

.yard-canvas {
  position: relative;
  min-height: 390px;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(39, 61, 82, 0.1) 1px, transparent 1px),
    linear-gradient(rgba(39, 61, 82, 0.1) 1px, transparent 1px),
    #f5f7f9;
  background-size: 36px 36px;
  border: 1px solid #aeb9c5;
  border-radius: 2px;
}

.yard-canvas::before {
  position: absolute;
  left: 8%;
  right: 8%;
  top: 54%;
  height: 18px;
  content: '';
  background: rgba(38, 56, 77, 0.18);
}

.gate-marker,
.sector-node {
  position: absolute;
  z-index: 2;
  transform: translate(-50%, -50%);
}

.gate-marker {
  left: 8%;
  top: 86%;
  padding: 5px 8px;
  color: #ffffff;
  background: #26384d;
  border: 1px solid #586b80;
  border-radius: 1px;
  font-weight: 700;
}

.sector-node {
  display: grid;
  width: 104px;
  height: 64px;
  place-items: center;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid #9fb0c0;
  border-radius: 2px;
}

.sector-node strong {
  font-size: 15px;
  font-weight: 700;
}

.sector-node small {
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 700;
}

.sector-node.working {
  border-color: var(--amber-500);
}

.sector-node.congested {
  border-color: var(--red-500);
}

.sector-node.selected {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.sector-node.selected small {
  color: #dceaff;
}

@media (max-width: 760px) {
  .yard-canvas {
    min-height: 320px;
  }

  .map-header,
  .map-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
