import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import CarrierDashboardView from '../views/carrier/CarrierDashboardView.vue'
import CarrierRequestsView from '../views/carrier/CarrierRequestsView.vue'
import CarrierApprovalsView from '../views/carrier/CarrierApprovalsView.vue'
import DriverDashboardView from '../views/driver/DriverDashboardView.vue'
import DriverGateStatusView from '../views/driver/DriverGateStatusView.vue'
import DriverVehiclesView from '../views/driver/DriverVehiclesView.vue'
import AdminMainView from '../views/admin/AdminMainView.vue'
import AdminDashboardView from '../views/admin/AdminDashboardView.vue'
import AdminMembersView from '../views/admin/AdminMembersView.vue'
import AdminTasksView from '../views/admin/AdminTasksView.vue'
import AdminGateLogsView from '../views/admin/AdminGateLogsView.vue'
import AdminContainersView from '../views/admin/AdminContainersView.vue'
import AdminEventsView from '../views/admin/AdminEventsView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
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
          path: 'vehicles',
          name: 'driver-vehicles',
          component: DriverVehiclesView,
          meta: { role: 'driver', title: '차량 등록' },
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
          path: 'members',
          name: 'admin-members',
          component: AdminMembersView,
          meta: { role: 'admin', title: '가입 인원관리' },
        },
        {
          path: 'tasks',
          name: 'admin-tasks',
          component: AdminTasksView,
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
    },
  ],
})

router.beforeEach((to) => {
  if (to.path === '/login') {
    return true
  }

  const user = JSON.parse(localStorage.getItem('portGateUser') || 'null')
  if (!user) {
    return '/login'
  }

  const roleRoot = {
    CARRIER: '/carrier',
    DRIVER: '/driver',
    ADMIN: '/admin',
  }[user.roleCode]

  if (roleRoot && !to.path.startsWith(roleRoot)) {
    return roleRoot === '/admin' ? '/admin/main' : `${roleRoot}/dashboard`
  }

  return true
})

export default router
