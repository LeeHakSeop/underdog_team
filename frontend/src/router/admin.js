import AdminMainView from '@/views/admin/AdminMainView.vue'
import AdminDashboardView from '@/views/admin/AdminDashboardView.vue'
import AdminWorkOrdersView from '@/views/admin/AdminWorkOrdersView.vue'
import AdminEventsView from '@/views/admin/AdminEventsView.vue'
import AdminMembersView from '@/views/admin/AdminMembersView.vue'
import AdminPlateRecognitionView from '@/views/admin/AdminPlateRecognitionView.vue'
import AdminYardMapView from '@/views/admin/AdminYardMapView.vue'
import AdminContainersView from '@/views/admin/AdminContainersView.vue'

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
        meta: { role: 'ADMIN', title: '상황 관제판' },
      },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: AdminDashboardView,
        meta: { role: 'ADMIN', title: '운영 현황 요약' },
      },
      {
        path: 'yard-map',
        name: 'admin-yard-map',
        component: AdminYardMapView,
        meta: { role: 'ADMIN', title: '운영 맵' },
      },
      {
        path: 'plate-recognition',
        name: 'admin-plate-recognition',
        component: AdminPlateRecognitionView,
        meta: { role: 'ADMIN', title: 'AI 번호판 인식' },
      },
      {
        path: 'members',
        name: 'admin-members',
        component: AdminMembersView,
        meta: { role: 'ADMIN', title: '회원 관리' },
      },
      {
        path: 'containers',
        name: 'admin-containers',
        component: AdminContainersView,
        meta: { role: 'ADMIN', title: '컨테이너 관리' },
      },
      {
        path: 'work-orders',
        name: 'admin-work-orders',
        component: AdminWorkOrdersView,
        meta: { role: 'ADMIN', title: '작업 관리' },
      },
      {
        path: 'events',
        name: 'admin-events',
        component: AdminEventsView,
        meta: { role: 'ADMIN', title: '알림/이벤트' },
      },
    ],
  },
]
