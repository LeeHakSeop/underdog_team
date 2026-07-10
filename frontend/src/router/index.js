import { createRouter, createWebHistory } from 'vue-router'

import authRoutes from './auth'
import adminRoutes from './admin'
import carrierRoutes from './carrier'
import driverRoutes from './driver'

import { authGuard } from './guard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),

  routes: [
    {
      path: '/',
      redirect: '/login',
    },

    ...authRoutes,
    ...adminRoutes,
    ...carrierRoutes,
    ...driverRoutes,

    {
<<<<<<< HEAD
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { shell: false },
    },
    {
      path: '/carrier',
      redirect: '/carrier/dashboard',
      meta: { role: 'carrier' },
      children: [
        {
          path: 'dashboard',
          name: 'carrier-dashboard',
          component: CarrierDashboardView,
          meta: { role: 'carrier', title: '운송사 업무' },
        },
        {
          path: 'driver-approval',
          name: 'carrier-driver-approval',
          component: CarrierApprovalDriverView,
          meta: { role: 'carrier', title: '기사 가입 승인' },
        },
        {
          path: 'vehicle-register',
          name: 'carrier-vehicle-register',
          component: CarrierVehicleRegisterView,
          meta: { role: 'carrier', title: '트레일러 배정' },
        },
        {
          path: 'requests',
          name: 'carrier-requests',
          component: CarrierRequestsView,
          meta: { role: 'carrier', title: '운송 요청' },
        },
        {
          path: 'approvals',
          name: 'carrier-approvals',
          component: CarrierApprovalsView,
          meta: { role: 'carrier', title: '승인 현황' },
        },
      ],
    },
    {
      path: '/driver',
      redirect: '/driver/dashboard',
      meta: { role: 'driver' },
      children: [
        {
          path: 'dashboard',
          name: 'driver-dashboard',
          component: DriverDashboardView,
          meta: { role: 'driver', title: '기사 작업' },
        },
        {
          path: 'work-status',
          name: 'driver-work-status',
          component: DriverGateStatusView,
          meta: { role: 'driver', title: '작업 현황' },
        },
        {
          path: 'gate-status',
          redirect: '/driver/work-status',
        },
        {
          path: 'sector-guide',
          redirect: '/driver/dashboard',
        },
        {
          path: 'vehicles',
          name: 'driver-vehicles',
          component: DriverVehiclesView,
          meta: { role: 'driver', title: '내 차량' },
        },
      ],
    },
    {
      path: '/admin',
      redirect: '/admin/main',
      meta: { role: 'admin' },
      children: [
        {
          path: 'main',
          name: 'admin-main',
          component: AdminMainView,
          meta: { role: 'admin', title: '관리자 메인' },
        },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: AdminDashboardView,
          meta: { role: 'admin', title: '센터 현황' },
        },
        {
          path: 'plate-recognition',
          name: 'admin-plate-recognition',
          component: AdminPlateRecognitionView,
          meta: { role: 'admin', title: 'AI 번호판 인식' },
        },
        {
          path: 'members',
          name: 'admin-members',
          component: AdminMembersView,
          meta: { role: 'admin', title: '가입 회원 관리' },
        },
        {
          path: 'work-orders',
          name: 'admin-work-orders',
          component: AdminWorkOrdersView,
          meta: { role: 'admin', title: '작업 관리' },
        },
        {
          path: 'gate-logs',
          name: 'admin-gate-logs',
          component: AdminGateLogsView,
          meta: { role: 'admin', title: '차량 출입 조회' },
        },
        {
          path: 'containers',
          name: 'admin-containers',
          component: AdminContainersView,
          meta: { role: 'admin', title: '컨테이너 조회' },
        },
        {
          path: 'events',
          name: 'admin-events',
          component: AdminEventsView,
          meta: { role: 'admin', title: '알림/이벤트' },
        },
      ],
=======
      path: '/:pathMatch(.*)*',
      redirect: '/login',
>>>>>>> origin/KBH
    },
  ],
})

router.beforeEach(authGuard)

export default router