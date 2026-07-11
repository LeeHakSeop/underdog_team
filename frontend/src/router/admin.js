import AdminMainView from '@/views/admin/AdminMainView.vue'
import AdminDashboardView from '@/views/admin/AdminDashboardView.vue'
import AdminWorkOrdersView from '@/views/admin/AdminWorkOrdersView.vue'
import AdminGateLogsView from '@/views/admin/AdminGateLogsView.vue'
import AdminContainersView from '@/views/admin/AdminContainersView.vue'
import AdminEventsView from '@/views/admin/AdminEventsView.vue'
import AdminMembersView from '@/views/admin/AdminMembersView.vue'
import AdminPlateRecognitionView from '@/views/admin/AdminPlateRecognitionView.vue'

export default [
  {
    path: '/admin',
    redirect: '/admin/main',
    meta: { role: 'ADMIN' },
    children: [
      {
        path: 'main',
        name: 'admin-main',
        component: AdminMainView,
        meta: { role: 'ADMIN', title: '관리자 메인' },
      },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: AdminDashboardView,
        meta: { role: 'ADMIN', title: '센터 현황' },
      },
      {
        path: 'members',
        name: 'admin-members',
        component: AdminMembersView,
        meta: { role: 'ADMIN', title: '가입 회원 관리' },
      },
      {
        path: 'work-orders',
        name: 'admin-work-orders',
        component: AdminWorkOrdersView,
        meta: { role: 'ADMIN', title: '작업 관리' },
      },
      {
        path: 'tasks',
        redirect: '/admin/work-orders',
      },
      {
        path: 'gate-logs',
        name: 'admin-gate-logs',
        component: AdminGateLogsView,
        meta: { role: 'ADMIN', title: '차량 출입 조회' },
      },
      {
        path: 'containers',
        name: 'admin-containers',
        component: AdminContainersView,
        meta: { role: 'ADMIN', title: '컨테이너 조회' },
      },
      {
        path: 'events',
        name: 'admin-events',
        component: AdminEventsView,
        meta: { role: 'ADMIN', title: '알림/이벤트' },
      },
      {
        path: 'plate-recognition',
        name: 'plate-recognition',
        component: AdminPlateRecognitionView,
        meta: { role: 'ADMIN', title: '인식조회' },
      },
    ],
  },
]
