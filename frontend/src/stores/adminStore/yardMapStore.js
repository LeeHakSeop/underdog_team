import { defineStore } from 'pinia'
import { yardMapLayout } from '@/config/yardMapLayout'
import { fetchYardMapSnapshot } from '@/api/adminApi/yardMapApi'

const sortNewestFirst = (left, right) => (right.gateLogId || 0) - (left.gateLogId || 0)

export const useYardMapStore = defineStore('yardMap', {
  state: () => ({
    containers: [],
    gateLogs: [],
    yardSectors: [],
    loading: false,
    error: '',
  }),

  getters: {
    containerCountBySectorId: (state) => state.containers.reduce((counts, container) => {
      counts.set(container.sectorId, (counts.get(container.sectorId) || 0) + 1)
      return counts
    }, new Map()),

    blockSummary: (state) => yardMapLayout.sectorBlocks.map((block) => {
      const sectors = state.yardSectors.filter((sector) => sector.blockName === block.sectorName)
      const sectorIds = new Set(sectors.map((sector) => sector.sectorId))

      return {
        ...block,
        sectorCount: sectors.length,
        containerCount: state.containers.filter((container) => sectorIds.has(container.sectorId)).length,
        waitingVehicleCount: sectors.reduce((total, sector) => total + (sector.waitingVehicleCount || 0), 0),
      }
    }),

    gateSummary: (state) => {
      const configuredGates = new Map(yardMapLayout.gates.map((gate) => [gate.gateNumber, gate]))
      const logsByGateNumber = new Map()

      state.gateLogs.forEach((log) => {
        const gateNumber = log.gateNumber || 'UNASSIGNED'
        logsByGateNumber.set(gateNumber, [...(logsByGateNumber.get(gateNumber) || []), log])
      })

      const mapped = yardMapLayout.gates.map((gate) => {
        const latestLog = [...(logsByGateNumber.get(gate.gateNumber) || [])].sort(sortNewestFirst)[0] || null
        return { ...gate, latestLog }
      })
      const unmapped = [...logsByGateNumber.entries()]
        .filter(([gateNumber]) => !configuredGates.has(gateNumber))
        .map(([gateNumber, logs]) => {
          const latestLog = [...logs].sort(sortNewestFirst)[0] || null
          return {
            gateNumber,
            gateName: latestLog?.gateName || '위치 미설정 게이트',
            direction: latestLog?.inOutType || '-',
            position: null,
            latestLog,
          }
        })

      return [...mapped, ...unmapped]
    },

  },

  actions: {
    async loadYardMap() {
      this.loading = true
      this.error = ''

      const snapshot = await fetchYardMapSnapshot()

      if (snapshot.containers) this.containers = snapshot.containers
      if (snapshot.gateLogs) this.gateLogs = snapshot.gateLogs
      if (snapshot.yardSectors) this.yardSectors = snapshot.yardSectors

      if (snapshot.hasError) {
        this.error = '일부 현황 데이터를 불러오지 못했습니다. 기존 데이터가 있으면 계속 표시합니다.'
      }

      this.loading = false
    },
  },
})
