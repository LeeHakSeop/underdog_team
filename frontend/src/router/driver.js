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
    ],
  },
]
