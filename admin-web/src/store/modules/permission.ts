import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { RouteRecordRaw } from 'vue-router'

import { getMenus } from '@/api/auth'
import router from '@/router'
import type { MenuVO } from '@/types/menu'

/** 把 @/views 下的所有页面组件收集起来,按后端 component 字符串映射 */
const viewModules = import.meta.glob('@/views/**/*.vue')

function resolveView(component: string) {
  const loader = viewModules[`/src/views/${component}.vue`]
  if (!loader) {
    console.warn(`[router] 未找到视图组件: /src/views/${component}.vue`)
  }
  return loader ?? (() => import('@/views/error/404.vue'))
}

/** 菜单节点 → 路由记录。DIR 无 component 只做目录,MENU 映射 views 下页面 */
export function menuToRoute(menu: MenuVO): RouteRecordRaw {
  const children = (menu.children ?? [])
    .filter((child) => child.type !== 'BUTTON')
    .map(menuToRoute)

  const route = {
    path: menu.path,
    name: menu.name,
    component: menu.component ? resolveView(menu.component) : undefined,
    meta: {
      title: menu.title,
      icon: menu.icon ?? undefined,
      hidden: menu.visible === false || menu.visible === 0,
    },
    children,
  }
  return route as RouteRecordRaw
}

/** 菜单驱动之外的补充路由(详情页等),挂在 Layout 下且不显示在侧边栏 */
const extraRoutes: RouteRecordRaw[] = [
  {
    path: '/order/detail/:id',
    name: 'OrderDetail',
    component: () => import('@/views/order/detail.vue'),
    meta: { title: '订单详情', hidden: true },
  },
]

export const usePermissionStore = defineStore('permission', () => {
  /** 后端原始菜单树(渲染侧边栏用) */
  const menus = ref<MenuVO[]>([])
  /** 动态路由是否已注册 */
  const loaded = ref(false)

  /** 侧边栏菜单:去掉 BUTTON 和隐藏项 */
  const sidebarMenus = computed(() => filterSidebar(menus.value))

  function filterSidebar(list: MenuVO[]): MenuVO[] {
    return list
      .filter(
        (m) => m.type !== 'BUTTON' && m.visible !== false && m.visible !== 0,
      )
      .map((m) => ({ ...m, children: filterSidebar(m.children ?? []) }))
      .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
  }

  /** 拉菜单并注册动态路由,最后注册 404 通配 */
  async function buildRoutes(): Promise<void> {
    const menuTree = (await getMenus()) ?? []
    menus.value = menuTree

    menuTree
      .filter((m) => m.type !== 'BUTTON')
      .forEach((m) => router.addRoute('Layout', menuToRoute(m)))

    extraRoutes.forEach((r) => router.addRoute('Layout', r))

    // 404 通配必须最后注册,避免刷新时动态路由未就绪被兜底
    router.addRoute({
      path: '/:pathMatch(.*)*',
      name: 'CatchAll',
      redirect: '/404',
      meta: { hidden: true },
    })

    loaded.value = true
  }

  function reset(): void {
    menus.value = []
    loaded.value = false
  }

  return { menus, loaded, sidebarMenus, buildRoutes, reset }
})
