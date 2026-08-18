import { defineStore } from 'pinia'
import { yardMapLayout } from '@/config/yardMapLayout'
import { fetchYardMapSnapshot } from '@/api/adminApi/yardMapApi'
import { fetchYardCongestion } from '@/api/adminApi/yardCongestionApi'
import { updateYardSectorCapacity } from '@/api/adminApi/yardSectorApi'

const fallbackGateName = '위치 미설정 게이트'

const gateAliasMap = {
  'G-IN-01': 'G01',
  'G-IN-02': 'G03',
  'G-OUT-01': 'G02',
  'G-OUT-02': 'G04',
}

const canonicalGateNumber = (gate) => {
  const gateNumber = String(gate?.gateNumber || '').trim().toUpperCase()
  const direction = String(gate?.direction || '').trim().toUpperCase()

  if (gateNumber === 'G01' && direction === 'OUT') return 'G02'
  if (gateNumber === 'G03' && direction === 'OUT') return 'G04'
  return gateAliasMap[gateNumber] || gateNumber
}

const gateEventTime = (gate) => {
  const value = gate?.latestExitTime || gate?.latestEntryTime
  const timestamp = value ? new Date(value).getTime() : 0
  return Number.isFinite(timestamp) ? timestamp : 0
}

const normalizeGateSummary = (gates) => {
  const normalized = new Map()

  gates.forEach((gate) => {
    const gateNumber = canonicalGateNumber(gate)
    if (!gateNumber) return

    const current = normalized.get(gateNumber)
    const todayInCount = Number(current?.todayInCount || 0) + Number(gate.todayInCount || 0)
    const todayOutCount = Number(current?.todayOutCount || 0) + Number(gate.todayOutCount || 0)
    const latest = !current || gateEventTime(gate) >= gateEventTime(current) ? gate : current

    normalized.set(gateNumber, {
      ...latest,
      gateNumber,
      todayInCount,
      todayOutCount,
    })
  })

  return normalized
}

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
      const gatesByNumber = normalizeGateSummary(state.gates)
      const configuredNumbers = new Set(yardMapLayout.gates.map((gate) => gate.gateNumber))

      const configured = yardMapLayout.gates.map((gate) => ({
        ...gate,
        ...(gatesByNumber.get(gate.gateNumber) || {}),
        gateNumber: gate.gateNumber,
        gateName: gate.gateName,
        direction: gate.direction,
        position: gate.position,
      }))

      const unmapped = [...gatesByNumber.values()]
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
        const [snapshot, congestion] = await Promise.all([
          fetchYardMapSnapshot(),
          fetchYardCongestion(),
        ])
        const congestionBySectorId = new Map((congestion?.sectors || []).map((sector) => [sector.sectorId, sector]))
        const snapshotSectors = snapshot?.sectors || []

        this.yardSectors = snapshotSectors.length > 0
          ? snapshotSectors.map((sector) => ({
            ...sector,
            ...(congestionBySectorId.get(sector.sectorId) || {}),
          }))
          : congestion?.sectors || []
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

    async updateSectorCapacity(sectorId, capacity) {
      const updated = await updateYardSectorCapacity(sectorId, capacity)
      this.yardSectors = this.yardSectors.map((sector) => (
        sector.sectorId === sectorId ? { ...sector, ...updated } : sector
      ))
      await this.loadYardMap()
      return updated
    },
  },
})
