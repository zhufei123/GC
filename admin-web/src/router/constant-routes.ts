import type { RouteRecordRaw } from 'vue-router'

import Layout from '@/layout/index.vue'

/** 静态路由:仅 /login、/404 与 Layout 空壳,业务路由由后端菜单动态注册 */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', hidden: true },
  },
  {
    path: '/',
    name: 'Layout',
    component: Layout,
    redirect: '/dashboard',
    children: [],
  },
]
