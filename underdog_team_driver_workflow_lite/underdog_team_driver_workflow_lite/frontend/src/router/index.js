import { createRouter, createWebHistory } from 'vue-router'

import authRoutes from './auth'
import adminRoutes from './admin'
import carrierRoutes from './carrier'
import driverRoutes from './driver'

import { authGuard } from './guard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),

  routes: [
    {
      path: '/',
      redirect: '/login',
    },

    ...authRoutes,
    ...adminRoutes,
    ...carrierRoutes,
    ...driverRoutes,

    {
      path: '/:pathMatch(.*)*',
      redirect: '/login',
    },
  ],
})

router.beforeEach(authGuard)

export default router