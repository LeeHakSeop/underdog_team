import AdminMainView from '@/views/admin/AdminMainView.vue'
import AdminDashboardView from '@/views/admin/AdminDashboardView.vue'
import AdminWorkOrdersView from '@/views/admin/AdminWorkOrdersView.vue'
import AdminGateLogsView from '@/views/admin/AdminGateLogsView.vue'
import AdminContainersView from '@/views/admin/AdminContainersView.vue'
import AdminEventsView from '@/views/admin/AdminEventsView.vue'
import AdminMembersView from '@/views/admin/AdminMembersView.vue'
import AdminPlateRecognitionView from '@/views/admin/AdminPlateRecognitionView.vue'
import AdminYardMapView from '@/views/admin/AdminYardMapView.vue'

export default [
  {
    path: '/admin',
    redirect: '/admin/main',
    meta: { role: 'admin' },
    children: [
      {
        path: 'main',
        name: 'admin-main',
        component: AdminMainView,
<<<<<<< HEAD
        meta: { role: 'admin', title: '관리자 메인' },
=======
        meta: { role: 'ADMIN', title: '관제 상황판' },
>>>>>>> origin/pjh
      },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: AdminDashboardView,
<<<<<<< HEAD
        meta: { role: 'admin', title: '센터 현황' },
      },
      {
        path: 'yard-map',
        name: 'admin-yard-map',
        component: AdminYardMapView,
        meta: { role: 'admin', title: '감만부두 운영 맵' },
      },
      {
        path: 'plate-recognition',
        name: 'admin-plate-recognition',
        component: AdminPlateRecognitionView,
        meta: { role: 'admin', title: 'AI 번호판 인식' },
=======
        meta: { role: 'ADMIN', title: '통계 요약' },
>>>>>>> origin/pjh
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
        meta: { role: 'admin', title: '작업 관리' },
      },
      {
        path: 'tasks',
        redirect: '/admin/work-orders',
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
<<<<<<< HEAD
        meta: { role: 'admin', title: '알림/이벤트' },
=======
        meta: { role: 'ADMIN', title: '알림/이벤트' },
      },
      {
        path: 'plate-recognition',
        name: 'plate-recognition',
        component: AdminPlateRecognitionView,
        meta: { role: 'ADMIN', title: 'AI 번호판 인식' },
>>>>>>> origin/pjh
      },
    ],
  },
]
