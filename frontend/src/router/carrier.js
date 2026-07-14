import CarrierDashboardView from '@/views/carrier/CarrierDashboardView.vue'
import CarrierApprovalDriverView from '@/views/carrier/CarrierApprovalDriverView.vue'
import CarrierVehicleRegisterView from '@/views/carrier/CarrierVehicleRegisterView.vue'
import CarrierApprovalsView from '@/views/carrier/CarrierApprovalsView.vue'
import CarrierWorkOrdersView from '@/views/carrier/CarrierWorkOrdersView.vue'

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
        path: 'work-orders',
        name: 'carrier-work-orders',
        component: CarrierWorkOrdersView,
        meta: {
          role: 'CARRIER',
          title: '기사 작업지시',
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
