import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  getMe,
  login,
  logout,
  type AdminInfo,
  type LoginParams,
} from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const admin = ref<AdminInfo | null>(null)
  const roles = ref<string[]>([])
  const perms = ref<string[]>([])

  const nickname = computed(
    () => admin.value?.nickname || admin.value?.username || '管理员',
  )
  const isSuper = computed(() => perms.value.includes('*:*:*'))

  /** 是否拥有某权限码(超管 *:*:* 全通过) */
  function hasPerm(perm: string | string[]): boolean {
    if (isSuper.value) return true
    const required = Array.isArray(perm) ? perm : [perm]
    return required.some((p) => perms.value.includes(p))
  }

  async function loginAction(params: LoginParams): Promise<void> {
    const data = await login(params)
    token.value = data.token
    setToken(data.token)
    admin.value = data.admin
    roles.value = data.roles ?? []
    perms.value = data.perms ?? []
  }

  /** 拉取当前用户,兼容 { admin, roles, perms } 与平铺结构 */
  async function fetchMe(): Promise<void> {
    const data = await getMe()
    admin.value =
      data.admin ??
      ({
        id: data.id ?? '',
        username: data.username ?? '',
        nickname: data.nickname ?? data.username ?? '管理员',
        avatar: data.avatar,
      } satisfies AdminInfo)
    roles.value = data.roles ?? []
    perms.value = data.perms ?? []
  }

  function reset(): void {
    token.value = ''
    admin.value = null
    roles.value = []
    perms.value = []
    removeToken()
  }

  async function logoutAction(): Promise<void> {
    try {
      await logout()
    } catch {
      // 登出接口失败不阻塞本地清态
    }
    reset()
  }

  return {
    token,
    admin,
    roles,
    perms,
    nickname,
    isSuper,
    hasPerm,
    loginAction,
    fetchMe,
    logoutAction,
    reset,
  }
})
