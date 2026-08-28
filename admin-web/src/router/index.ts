import { createRouter, createWebHistory } from 'vue-router'

import { constantRoutes } from './constant-routes'

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

export default router
