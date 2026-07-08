/*
=========================================
운송사(Carrier) Router
=========================================

역할
- 운송사 메인
- 운송 요청
- 승인 현황

접근 권한
CARRIER
=========================================
*/

import CarrierDashboardView from '@/views/carrier/CarrierDashboardView.vue'
import CarrierRequestsView from '@/views/carrier/CarrierRequestsView.vue'
import CarrierApprovalsView from '@/views/carrier/CarrierApprovalsView.vue'

export default [
  {
    path: '/carrier',
    redirect: '/carrier/dashboard',
    meta: { role: 'CARRIER' },
    children: [
      {
        path: 'dashboard',
        name: 'carrier-dashboard',
        component: CarrierDashboardView,
        meta: { role: 'CARRIER', title: '운송사 업무' },
      },
      {
        path: 'requests',
        name: 'carrier-requests',
        component: CarrierRequestsView,
        meta: { role: 'CARRIER', title: '운송 요청' },
      },
      {
        path: 'approvals',
        name: 'carrier-approvals',
        component: CarrierApprovalsView,
        meta: { role: 'CARRIER', title: '승인 현황' },
      },
    ],
  },
]
