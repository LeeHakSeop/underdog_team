/*
=========================================
기사(Driver) Router
=========================================

역할
- 기사 메인
- 게이트 상태
- 작업 진행
- 섹터 안내

접근 권한
DRIVER
=========================================
*/

import DriverDashboardView from '@/views/driver/DriverDashboardView.vue'
import DriverGateStatusView from '@/views/driver/DriverGateStatusView.vue'
import DriverVehiclesView from '@/views/driver/DriverVehiclesView.vue'

export default [
  {
    path: '/driver',
    redirect: '/driver/dashboard',
    meta: { role: 'DRIVER' },
    children: [
      {
        path: 'dashboard',
        name: 'driver-dashboard',
        component: DriverDashboardView,
        meta: { role: 'DRIVER', title: '기사 작업' },
      },
      {
        path: 'work-status',
        name: 'driver-work-status',
        component: DriverGateStatusView,
        meta: { role: 'DRIVER', title: '작업 현황' },
      },
      {
        path: 'vehicles',
        name: 'driver-vehicles',
        component: DriverVehiclesView,
        meta: { role: 'DRIVER', title: '차량 등록' },
      },
      {
        path: 'gate-status',
        redirect: '/driver/work-status',
      },
      {
        path: 'sector-guide',
        redirect: '/driver/dashboard',
      },
<<<<<<< HEAD
      {
        path: 'vehicles',
        name: 'driver-vehicles',
        component: DriverVehiclesView,
        meta: { role: 'DRIVER', title: '내 차량' },
      },
=======
>>>>>>> origin/KBH
    ],
  },
]