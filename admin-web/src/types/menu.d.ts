export type MenuType = 'DIR' | 'MENU' | 'BUTTON'

/** 后端 /admin-api/auth/menus 菜单节点 */
export interface MenuVO {
  id: string
  parentId?: string
  /** 路由 name(唯一) */
  name: string
  /** 菜单标题 */
  title: string
  type: MenuType
  /** 一级为绝对路径 /goods,子级为相对路径 category */
  path: string
  /** MENU 才有,如 goods/category/index 对应 @/views/goods/category/index.vue */
  component?: string | null
  icon?: string | null
  perms?: string | null
  sort?: number
  visible?: boolean | number
  status?: number
  children?: MenuVO[]
}
