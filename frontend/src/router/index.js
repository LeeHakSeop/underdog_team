/*
=========================================
Router 메인 설정
=========================================

역할
- Vue Router 생성
- 각 역할별 Router 등록
- 로그인/권한 검사(Guard) 연결

관리 파일
- auth.js
- admin.js
- carrier.js
- driver.js
- guard.js
=========================================
*/

import { createRouter, createWebHistory } from 'vue-router'

import authRoutes from './auth'
import adminRoutes from './admin'
import carrierRoutes from './carrier'
import driverRoutes from './driver'
import vehicleRoutes from './vehicle'

import { authGuard } from './guard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    ...authRoutes,
    ...adminRoutes,
    ...carrierRoutes,
    ...driverRoutes,
    ...vehicleRoutes,
  ],
})

// 개발중에 OFF
// router.beforeEach(authGuard)

export default router