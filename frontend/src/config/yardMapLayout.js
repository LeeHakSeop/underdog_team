// Operating map layout is kept on the frontend because these are display
// coordinates, not operational DB data. Adjust only these coordinates when
// the real gate or yard block positions are finalized.
export const yardMapLayout = {
  center: [35.10535, 129.0846],
  zoom: 16,
  gates: [
    { gateNumber: 'G01', gateName: '감만부두 입차 게이트 1', position: [35.10766, 129.07897], direction: 'IN' },
    { gateNumber: 'G03', gateName: '감만부두 입차 게이트 2', position: [35.10734, 129.07896], direction: 'IN' },
    { gateNumber: 'G02', gateName: '감만부두 출차 게이트 1', position: [35.10766, 129.07858], direction: 'OUT' },
    { gateNumber: 'G04', gateName: '감만부두 출차 게이트 2', position: [35.10734, 129.07857], direction: 'OUT' },
  ],
  sectorBlocks: [
    { sectorName: 'A', label: 'A 구역', center: [35.105358, 129.080955], widthMeters: 340, heightMeters: 290 },
    { sectorName: 'B', label: 'B 구역', center: [35.105350, 129.084635], widthMeters: 305, heightMeters: 290 },
    { sectorName: 'C', label: 'C 구역', center: [35.105358, 129.088197], widthMeters: 325, heightMeters: 290 },
  ],
}
