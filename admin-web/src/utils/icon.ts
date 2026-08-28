import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const iconNames = new Set(Object.keys(ElementPlusIconsVue))

/** 后端菜单 icon 字符串 → 已注册的 Element Plus 图标组件名,非法时回退 */
export function resolveIcon(name?: string | null, fallback = 'Menu'): string {
  return name && iconNames.has(name) ? name : fallback
}
