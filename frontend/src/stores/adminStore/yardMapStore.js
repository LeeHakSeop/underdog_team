import { defineStore } from 'pinia'
import { yardMapLayout } from '@/config/yardMapLayout'
import { fetchYardMapSnapshot } from '@/api/adminApi/yardMapApi'

const fallbackGateName = '위치 미설정 게이트'

export const useYardMapStore = defineStore('yardMap', {
  state: () => ({
    gates: [],
    vehicles: [],
    yardSectors: [],
    loading: false,
    error: '',
    lastUpdatedAt: null,
    failedAt: null,
    failureCount: 0,
    stale: false,
  }),

  getters: {
    containerCountBySectorId: (state) => state.yardSectors.reduce((counts, sector) => {
      counts.set(sector.sectorId, sector.containerCount || 0)
      return counts
    }, new Map()),

    vehicleCountBySectorId: (state) => state.vehicles.reduce((counts, vehicle) => {
      if (!vehicle.sectorId) return counts
      counts.set(vehicle.sectorId, (counts.get(vehicle.sectorId) || 0) + 1)
      return counts
    }, new Map()),

    blockSummary: (state) => yardMapLayout.sectorBlocks.map((block) => {
      const sectors = state.yardSectors.filter((sector) => sector.blockName === block.sectorName)
      const sectorIds = new Set(sectors.map((sector) => sector.sectorId))

      return {
        ...block,
        sectorCount: sectors.length,
        containerCount: sectors.reduce((total, sector) => total + (sector.containerCount || 0), 0),
        waitingVehicleCount: sectors.reduce((total, sector) => total + (sector.waitingVehicleCount || 0), 0),
        workOrderCount: sectors.reduce((total, sector) => total + (sector.workOrderCount || 0), 0),
        vehicleCount: state.vehicles.filter((vehicle) => sectorIds.has(vehicle.sectorId)).length,
      }
    }),

    gateSummary: (state) => {
      const gatesByNumber = new Map(state.gates.map((gate) => [gate.gateNumber, gate]))
      const configuredNumbers = new Set(yardMapLayout.gates.map((gate) => gate.gateNumber))

      const configured = yardMapLayout.gates.map((gate) => ({
        ...gate,
        ...(gatesByNumber.get(gate.gateNumber) || {}),
        gateName: gatesByNumber.get(gate.gateNumber)?.gateName || gate.gateName,
        direction: gate.direction,
        position: gate.position,
      }))

      const unmapped = state.gates
        .filter((gate) => !configuredNumbers.has(gate.gateNumber))
        .map((gate) => ({
          ...gate,
          gateName: gate.gateName || fallbackGateName,
          direction: gate.direction || '-',
          position: null,
        }))

      return [...configured, ...unmapped]
    },
  },

  actions: {
    async loadYardMap() {
      this.loading = true

      try {
        const snapshot = await fetchYardMapSnapshot()
        this.yardSectors = snapshot?.sectors || []
        this.gates = snapshot?.gates || []
        this.vehicles = snapshot?.vehicles || []
        this.error = ''
        this.lastUpdatedAt = new Date().toISOString()
        this.failedAt = null
        this.failureCount = 0
        this.stale = false
      } catch (loadError) {
        this.error = loadError.message || '운영 맵 데이터를 불러오지 못했습니다.'
        this.failedAt = new Date().toISOString()
        this.failureCount += 1
        this.stale = this.lastUpdatedAt !== null
      } finally {
        this.loading = false
      }
    },
  },
})
