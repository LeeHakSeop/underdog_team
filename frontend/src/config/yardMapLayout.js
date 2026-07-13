// 지도 위에 고정으로 그릴 운영 배치도입니다.
// DB 데이터가 아니므로 게이트/섹터의 실제 위치가 확정되면 이 파일의 좌표만 조정하면 됩니다.
export const yardMapLayout = {
  center: [35.10535, 129.0846],
  zoom: 16,
  gates: [
    { gateNumber: 'G01', gateName: '동문 입차 게이트', position: [35.10765659762411,129.07896824913763], direction: 'IN' },
    { gateNumber: 'G02', gateName: '동문 출차 게이트', position: [35.107636061787346,129.07858939209657], direction: 'OUT' },
  ],
  // 감만부두 안쪽의 실제 큰 도로 구획을 기준으로 잡은 구역 외곽입니다.
  // 블록을 옮길 때는 꼭짓점 4개가 아닌 center의 위도·경도만 바꾸면 됩니다.
  // 각 구역 안에는 DB의 세부 섹터 20개를 4열 x 5행으로 표시합니다.
  sectorBlocks: [
    { sectorName: 'A', label: 'A 구역', center: [35.105358, 129.080955], widthMeters: 340, heightMeters: 290 },
    { sectorName: 'B', label: 'B 구역', center: [35.105350, 129.084635], widthMeters: 305, heightMeters: 290 },
    { sectorName: 'C', label: 'C 구역', center: [35.105358, 129.088197], widthMeters: 325, heightMeters: 290 },
  ],
}
