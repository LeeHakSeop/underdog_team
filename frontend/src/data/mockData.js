export const roleCards = [
  {
    role: 'carrier',
    title: '운송사 담당자',
    path: '/carrier/dashboard',
    description: '운송 작업 정보를 등록하고 관리자 승인 상태를 확인합니다.',
    metrics: ['승인 대기 4건', '기사 후보 7명'],
  },
  {
    role: 'driver',
    title: '화물 기사',
    path: '/driver/dashboard',
    description: '오늘 작업, 게이트 통과 상태, 섹터 이동 경로를 확인합니다.',
    metrics: ['현재 작업 1건', 'B-07 이동'],
  },
  {
    role: 'admin',
    title: '관리자',
    path: '/admin/dashboard',
    description: '물류센터 전체 차량 배치, 작업 현황, 섹터 상황을 관리합니다.',
    metrics: ['센터 내 차량 18대', '상차 진행 6건'],
  },
]

export const operationStats = [
  { label: '센터 내부 차량', value: 18, hint: '현재 입차 기준' },
  { label: '배차 대기', value: 9, hint: '운송사 요청' },
  { label: '기사 승낙', value: 4, hint: '섹터 배정 대상' },
  { label: '상차 진행', value: 6, hint: '장비 작업 중' },
]

export const workOrders = [
  {
    orderNo: 'WO-20260630-001',
    vehicleNo: '12가3456',
    driverName: '김도현',
    carrierName: '부산로지스',
    containerNo: 'KDTU1234567',
    sectorCode: 'B-07',
    status: '승인 완료',
    time: '13:00',
    workType: '반출 상차',
    cargoType: 'DRY',
  },
  {
    orderNo: 'WO-20260630-002',
    vehicleNo: '34나7890',
    driverName: '이서준',
    carrierName: '코리아프레이트',
    containerNo: 'KDTU7654321',
    sectorCode: 'C-03',
    status: '배차 대기',
    time: '14:00',
    workType: '반출 상차',
    cargoType: 'REEFER',
  },
  {
    orderNo: 'WO-20260630-003',
    vehicleNo: '98바2211',
    driverName: '박민재',
    carrierName: '동해운송',
    containerNo: 'KDTU3321904',
    sectorCode: 'A-01',
    status: '기사 승낙',
    time: '14:30',
    workType: '반입 하차',
    cargoType: 'TANK',
  },
  {
    orderNo: 'WO-20260630-004',
    vehicleNo: '67다5581',
    driverName: '정하늘',
    carrierName: '항만익스프레스',
    containerNo: 'KDTU8842109',
    sectorCode: 'A-02',
    status: '상차 진행',
    time: '15:00',
    workType: '반출 상차',
    cargoType: 'DRY',
  },
]

export const availableDrivers = [
  { name: '김도현', vehicleNo: '12가3456', distance: '1.8km', status: '대기 가능', score: 96 },
  { name: '오지훈', vehicleNo: '67다5581', distance: '3.2km', status: '30분 내 가능', score: 88 },
  { name: '정하늘', vehicleNo: '41마9027', distance: '4.6km', status: '대기 가능', score: 84 },
]

export const containers = [
  {
    containerNo: 'KDTU1234567',
    sizeType: '40FT',
    type: 'DRY',
    location: 'Block B / Bay 07 / Row 03 / Tier 02',
    sectorCode: 'B-07',
    release: '가능',
  },
  {
    containerNo: 'KDTU7654321',
    sizeType: '20FT',
    type: 'REEFER',
    location: 'Block C / Bay 03 / Row 01 / Tier 01',
    sectorCode: 'C-03',
    release: '가능',
  },
  {
    containerNo: 'KDTU3321904',
    sizeType: '40FT',
    type: 'TANK',
    location: 'Block A / Bay 01 / Row 02 / Tier 01',
    sectorCode: 'A-01',
    release: '보류',
  },
  {
    containerNo: 'KDTU8842109',
    sizeType: '40FT',
    type: 'DRY',
    location: 'Block A / Bay 02 / Row 04 / Tier 01',
    sectorCode: 'A-02',
    release: '가능',
  },
]

export const gateLogs = [
  {
    logNo: 'GL-001',
    vehicleNo: '12가3456',
    gate: '게이트 01',
    type: '입차',
    time: '13:02',
    result: '승인',
  },
  {
    logNo: 'GL-002',
    vehicleNo: '34나7890',
    gate: '게이트 01',
    type: '입차',
    time: '13:50',
    result: '대기',
  },
  {
    logNo: 'GL-003',
    vehicleNo: '98바2211',
    gate: '게이트 03',
    type: '출차',
    time: '15:10',
    result: '승인',
  },
  {
    logNo: 'GL-004',
    vehicleNo: '67다5581',
    gate: '게이트 02',
    type: '입차',
    time: '15:22',
    result: '승인',
  },
]

export const gateCameras = [
  {
    id: 'G-01',
    name: '게이트 01',
    gateType: 'IN',
    status: '대기',
    recognizedVehicleNo: '12가3456',
    matchedOrderNo: 'WO-20260630-001',
  },
  {
    id: 'G-02',
    name: '게이트 02',
    gateType: 'IN',
    status: '대기',
    recognizedVehicleNo: '34나7890',
    matchedOrderNo: 'WO-20260630-002',
  },
  {
    id: 'G-03',
    name: '게이트 03',
    gateType: 'OUT',
    status: '대기',
    recognizedVehicleNo: '98바2211',
    matchedOrderNo: 'WO-20260630-003',
  },
  {
    id: 'G-04',
    name: '게이트 04',
    gateType: 'OUT',
    status: '대기',
    recognizedVehicleNo: '',
    matchedOrderNo: '',
  },
]

export const yardSectors = [
  { code: 'A-01', x: 19, y: 27, status: 'available', statusText: '사용 가능', count: 1 },
  { code: 'A-02', x: 38, y: 27, status: 'working', statusText: '작업 중', count: 3 },
  { code: 'B-07', x: 58, y: 43, status: 'target', statusText: '배정 섹터', count: 2 },
  { code: 'B-08', x: 78, y: 43, status: 'available', statusText: '사용 가능', count: 0 },
  { code: 'C-03', x: 30, y: 69, status: 'congested', statusText: '대기 많음', count: 5 },
  { code: 'C-04', x: 68, y: 69, status: 'working', statusText: '작업 중', count: 4 },
]

export const driverActions = ['입차', '상차 시작', '상차 완료', '하차 시작', '하차 완료', '출차']

export const events = [
  { time: '13:02', type: '입차 승인', target: '12가3456', message: '작업 오더 WO-20260630-001 조회 완료' },
  { time: '13:50', type: '섹터 대기', target: 'C-03', message: '대체 대기구역 WAIT-C 안내 필요' },
  { time: '14:12', type: '상차 시작', target: 'B-07', message: '필드 장비 RS-02 작업 시작' },
  { time: '15:10', type: '출차 완료', target: '98바2211', message: '반출 이력 저장 완료' },
]

export const exceptions = [
  { type: '섹터 대기', vehicleNo: '34나7890', message: 'C-03 대기 차량 5대', status: '처리 중' },
  { type: '작업 없음', vehicleNo: '77라0910', message: '예약 정보 없음', status: '미처리' },
  { type: '컨테이너 보류', vehicleNo: '98바2211', message: '반출 보류 컨테이너 확인', status: '확인 필요' },
]

export const users = [
  { name: '배지홍', role: '관리자', area: '전체 시스템', status: '사용' },
  { name: '김도현', role: '화물 기사', area: 'B-07', status: '사용' },
  { name: '부산로지스', role: '운송사 담당자', area: '운송 요청', status: '사용' },
]
