import type { Directive, DirectiveBinding } from 'vue'

import { useUserStore } from '@/store/modules/user'

function check(el: HTMLElement, binding: DirectiveBinding<string | string[]>): void {
  const userStore = useUserStore()
  const value = binding.value
  if (!value || (Array.isArray(value) && value.length === 0)) return
  if (!userStore.hasPerm(value)) {
    el.parentNode?.removeChild(el)
  }
}

/**
 * 按钮级权限指令
 * 用法:v-permission="'recycle:category:add'" 或 v-permission="['a','b']"(任一命中即显示)
 * 超管 perms 含 *:*:* 时全部显示
 */
export const permission: Directive<HTMLElement, string | string[]> = {
  mounted: check,
}

export default permission
