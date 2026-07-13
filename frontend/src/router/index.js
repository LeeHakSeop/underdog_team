import { createRouter, createWebHistory } from 'vue-router'
<<<<<<< HEAD
import LoginView from '../views/LoginView.vue'
import CarrierDashboardView from '../views/carrier/CarrierDashboardView.vue'
import CarrierApprovalDriverView from '../views/carrier/CarrierApprovalDriverView.vue'
import CarrierVehicleRegisterView from '../views/carrier/CarrierVehicleRegisterView.vue'
import CarrierRequestsView from '../views/carrier/CarrierRequestsView.vue'
import CarrierApprovalsView from '../views/carrier/CarrierApprovalsView.vue'
import DriverDashboardView from '../views/driver/DriverDashboardView.vue'
import DriverGateStatusView from '../views/driver/DriverGateStatusView.vue'
import DriverVehiclesView from '../views/driver/DriverVehiclesView.vue'
import adminRoutes from './admin'
=======

import authRoutes from './auth'
import adminRoutes from './admin'
import carrierRoutes from './carrier'
import driverRoutes from './driver'

import { authGuard } from './guard'
>>>>>>> origin/hakseop

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
          meta: { role: 'driver', title: '차량 등록' },
        },
      ],
    },
    ...adminRoutes,
=======
      path: '/:pathMatch(.*)*',
      redirect: '/login',
    },
>>>>>>> origin/hakseop
  ],
})

router.beforeEach(authGuard)

export default router
