import router from '@/router'
import { usePermissionStore } from '@/store/modules/permission'
import { useUserStore } from '@/store/modules/user'
import { getToken } from '@/utils/auth'

const WHITE_LIST = ['/login', '/404']

router.beforeEach(async (to) => {
  const hasToken = !!getToken()

  if (!hasToken) {
    if (WHITE_LIST.includes(to.path)) return true
    return {
      path: '/login',
      query: to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : {},
      replace: true,
    }
  }

  // 已登录访问登录页 → 回首页
  if (to.path === '/login') {
    return { path: '/', replace: true }
  }

  const permissionStore = usePermissionStore()
  if (permissionStore.loaded) return true

  // 首次进入(或刷新):拉当前用户 + 菜单,动态注册路由后重进目标路由
  const userStore = useUserStore()
  try {
    await userStore.fetchMe()
    await permissionStore.buildRoutes()
    return { ...to, replace: true }
  } catch {
    userStore.reset()
    permissionStore.reset()
    return { path: '/login', query: { redirect: to.fullPath }, replace: true }
  }
})

router.afterEach((to) => {
  const title = to.meta.title
  document.title = title ? `${title} - 绿色回收管理后台` : '绿色回收管理后台'
})
