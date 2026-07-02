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