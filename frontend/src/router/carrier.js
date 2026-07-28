import CarrierDashboardView from '@/views/carrier/CarrierDashboardView.vue'
import CarrierApprovalDriverView from '@/views/carrier/CarrierApprovalDriverView.vue'
import CarrierInquiryView from '@/views/carrier/CarrierInquiryView.vue'
import CarrierInputView from '@/views/carrier/CarrierInputView.vue'

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
          title: '승인·회원 관리',
        },
      },

      {
        path: 'vehicle-register',
        name: 'carrier-vehicle-register',
        component: CarrierInputView,
        meta: {
          role: 'CARRIER',
          title: '배정·작업 입력 및 수정',
        },
      },

      {
        path: 'work-orders',
        name: 'carrier-work-orders',
        component: CarrierInquiryView,
        meta: {
          role: 'CARRIER',
          title: '배정·작업 조회',
        },
      },

      {
        path: 'input',
        name: 'carrier-input',
        component: CarrierInputView,
        meta: {
          role: 'CARRIER',
          title: '배정·작업 입력 및 수정',
        },
      },

      {
        path: 'inquiry',
        name: 'carrier-inquiry',
        component: CarrierInquiryView,
        meta: {
          role: 'CARRIER',
          title: '배정·작업 조회',
        },
      },

      {
        path: 'approvals',
        name: 'carrier-approvals',
        redirect: '/carrier/driver-approval',
        meta: {
          role: 'CARRIER',
          title: '승인·회원 관리',
        },
      },
    ],
  },
]
