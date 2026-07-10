import CarrierDashboardView from '@/views/carrier/CarrierDashboardView.vue'
import CarrierApprovalDriverView from '@/views/carrier/CarrierApprovalDriverView.vue'
import CarrierDriverManagementView from '@/views/carrier/CarrierDriverManagementView.vue'
import CarrierVehicleRegisterView from '@/views/carrier/CarrierVehicleRegisterView.vue'
import CarrierRequestsView from '@/views/carrier/CarrierRequestsView.vue'
import CarrierApprovalsView from '@/views/carrier/CarrierApprovalsView.vue'

export default [
  {
    path: '/carrier',
    redirect: '/carrier/dashboard',
    meta: {
      role: 'CARRIER',
    },

    children: [
      {
        path: 'dashboard',
        name: 'carrier-dashboard',
        component: CarrierDashboardView,
        meta: {
          role: 'CARRIER',
          title: '운송사 업무',
        },
      },

      {
        path: 'drivers',
        name: 'carrier-driver-management',
        component: CarrierDriverManagementView,
        meta: {
          role: 'CARRIER',
          title: '기사관리',
        },
      },

      {
        path: 'driver-approval',
        name: 'carrier-driver-approval',
        component: CarrierApprovalDriverView,
        meta: {
          role: 'CARRIER',
          title: '기사 가입 승인',
        },
      },

      {
        path: 'vehicle-register',
        name: 'carrier-vehicle-register',
        component: CarrierVehicleRegisterView,
        meta: {
          role: 'CARRIER',
          title: '트레일러 배정',
        },
      },

      {
        path: 'requests',
        name: 'carrier-requests',
        component: CarrierRequestsView,
        meta: {
          role: 'CARRIER',
          title: '운송 요청',
        },
      },

      {
        path: 'approvals',
        name: 'carrier-approvals',
        component: CarrierApprovalsView,
        meta: {
          role: 'CARRIER',
          title: '승인 현황',
        },
      },
    ],
  },
]