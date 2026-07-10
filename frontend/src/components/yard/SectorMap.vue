<script setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useYardSectorStore } from '@/stores/adminStore/yardSectorStore'

const props = defineProps({
  selectedCode: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['select'])
const yardSectorStore = useYardSectorStore()
const { yardSectors } = storeToRefs(yardSectorStore)

const gate = { x: 8, y: 86 }
const mapPositions = [
  { x: 19, y: 27 },
  { x: 38, y: 27 },
  { x: 58, y: 43 },
  { x: 78, y: 43 },
  { x: 30, y: 69 },
  { x: 68, y: 69 },
]

const mapSectors = computed(() => {
  return yardSectors.value.map((sector, index) => ({
    ...sector,
    code: sector.sectorName,
    count: sector.waitingVehicleCount || 0,
    statusText: sector.sectorStatus || '-',
    status: sector.sectorStatus === '혼잡' ? 'congested' : sector.sectorStatus === '작업중' ? 'working' : 'available',
    x: mapPositions[index % mapPositions.length].x,
    y: mapPositions[index % mapPositions.length].y,
  }))
})

const selectedSector = computed(() => {
  return mapSectors.value.find((sector) => sector.code === props.selectedCode) || mapSectors.value[0]
})

onMounted(() => {
  yardSectorStore.loadYardSectors()
})
</script>

<template>
  <div class="sector-map">
    <div class="map-header">
      <div>
        <b>섹터 안내</b>
        <span>섹터를 선택하면 진행 중인 작업이 표시됩니다.</span>
      </div>
      <span class="status-pill green">{{ selectedSector?.code }}</span>
    </div>

    <div class="yard-canvas">
      <div class="gate-marker" :style="{ left: `${gate.x}%`, top: `${gate.y}%` }">GATE</div>
      <div class="route-line route-line-a"></div>
      <div class="route-line route-line-b"></div>
      <button
        v-for="sector in mapSectors"
        :key="sector.sectorId"
        class="sector-node"
        :class="[sector.status, { selected: sector.code === selectedSector?.code }]"
        type="button"
        :style="{ left: `${sector.x}%`, top: `${sector.y}%` }"
        @click="emit('select', sector.code)"
      >
        <strong>{{ sector.code }}</strong>
        <small>{{ sector.count }}대</small>
      </button>
      <div class="truck-dot current">차량</div>
      <div class="truck-dot other one">대기</div>
      <div class="truck-dot other two">작업</div>
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

.yard-canvas::before,
.yard-canvas::after {
  position: absolute;
  content: '';
  background: rgba(38, 56, 77, 0.18);
  border-radius: 1px;
}

.yard-canvas::before {
  left: 8%;
  right: 8%;
  top: 54%;
  height: 18px;
}

.yard-canvas::after {
  left: 48%;
  top: 12%;
  width: 18px;
  bottom: 12%;
}

.gate-marker,
.sector-node,
.truck-dot {
  position: absolute;
  z-index: 2;
  transform: translate(-50%, -50%);
}

.gate-marker {
  padding: 5px 8px;
  color: #ffffff;
  background: #26384d;
  border: 1px solid #586b80;
  border-radius: 1px;
  font-weight: 700;
}

.sector-node {
  display: grid;
  width: 86px;
  height: 62px;
  place-items: center;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid #9fb0c0;
  border-radius: 2px;
  box-shadow: none;
}

.sector-node strong {
  font-size: 15px;
  font-weight: 700;
}

.sector-node small {
  color: var(--ink-500);
  font-weight: 700;
}

.sector-node.target {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.sector-node.target small {
  color: #dceaff;
}

.sector-node.congested {
  border-color: var(--red-500);
}

.sector-node.working {
  border-color: var(--amber-500);
}

.sector-node.selected {
  outline: 2px solid #23639c;
  outline-offset: 1px;
  box-shadow: none;
}

.route-line {
  position: absolute;
  z-index: 1;
  background: #6b879f;
  box-shadow: none;
}

.route-line-a {
  left: 8%;
  top: 84%;
  width: 50%;
  height: 5px;
}

.route-line-b {
  left: 56%;
  top: 43%;
  width: 5px;
  height: 42%;
}

.truck-dot {
  display: inline-flex;
  min-width: 54px;
  min-height: 23px;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  color: #ffffff;
  background: #2f7d57;
  border: 1px solid #73a087;
  border-radius: 1px;
  font-size: 11px;
  font-weight: 700;
}

.truck-dot.current {
  left: 38%;
  top: 84%;
}

.truck-dot.other {
  background: var(--ink-500);
}

.truck-dot.one {
  left: 62%;
  top: 56%;
}

.truck-dot.two {
  left: 72%;
  top: 68%;
}

@media (max-width: 760px) {
  .yard-canvas {
    min-height: 320px;
  }

  .sector-node {
    width: 70px;
    height: 54px;
  }

  .map-header,
  .map-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
