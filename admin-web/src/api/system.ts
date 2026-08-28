import request from './request'
import type { PageQuery, PageResult } from '@/types/api'
import type { MenuVO } from '@/types/menu'

/* ---------------- 管理员 ---------------- */

export interface AdminVO {
  id: string
  username: string
  nickname?: string
  phone?: string
  avatar?: string
  status?: number
  roleIds?: string[]
  roles?: Array<string | { id: string; name?: string }>
  roleNames?: string[]
  createdAt?: string
  createTime?: string
  lastLoginAt?: string
}

export interface AdminForm {
  username: string
  nickname: string
  password?: string
  phone?: string
  roleIds: string[]
  status: number
}

export function getAdminPage(params: PageQuery) {
  return request<PageResult<AdminVO>>({
    url: '/admin-api/system/admin/page',
    method: 'get',
    params,
  })
}

export function createAdmin(data: AdminForm) {
  return request<void>({ url: '/admin-api/system/admin', method: 'post', data })
}

export function updateAdmin(id: string, data: AdminForm) {
  return request<void>({ url: `/admin-api/system/admin/${id}`, method: 'put', data })
}

export function updateAdminStatus(id: string, status: number) {
  return request<void>({
    url: `/admin-api/system/admin/${id}/status`,
    method: 'put',
    data: { status },
  })
}

export function resetAdminPassword(id: string, password: string) {
  return request<void>({
    url: `/admin-api/system/admin/${id}/password`,
    method: 'put',
    data: { password },
  })
}

export function deleteAdmin(id: string) {
  return request<void>({ url: `/admin-api/system/admin/${id}`, method: 'delete' })
}

/* ---------------- 角色 ---------------- */

export interface RoleVO {
  id: string
  name: string
  code?: string
  remark?: string
  createdAt?: string
}

/** 后端 RoleSaveDTO 仅 code/name/remark(sys_role 无 status 字段) */
export interface RoleForm {
  name: string
  code: string
  remark?: string
}

/** 文档为 GET /admin-api/system/role,兼容返回数组或分页结构 */
export function getRoleList(params?: PageQuery) {
  return request<RoleVO[] | PageResult<RoleVO>>({
    url: '/admin-api/system/role',
    method: 'get',
    params,
  })
}

export function createRole(data: RoleForm) {
  return request<void>({ url: '/admin-api/system/role', method: 'post', data })
}

export function updateRole(id: string, data: RoleForm) {
  return request<void>({ url: `/admin-api/system/role/${id}`, method: 'put', data })
}

export function deleteRole(id: string) {
  return request<void>({ url: `/admin-api/system/role/${id}`, method: 'delete' })
}

/** 兼容返回 id 数组或 { menuIds } */
export function getRoleMenus(id: string) {
  return request<string[] | { menuIds?: string[] }>({
    url: `/admin-api/system/role/${id}/menus`,
    method: 'get',
  })
}

export function assignRoleMenus(id: string, menuIds: string[]) {
  return request<void>({
    url: `/admin-api/system/role/${id}/menus`,
    method: 'put',
    data: { menuIds },
  })
}

/* ---------------- 菜单 ---------------- */

export interface MenuForm {
  parentId: string
  name: string
  title: string
  type: 'DIR' | 'MENU' | 'BUTTON'
  path?: string
  component?: string
  icon?: string
  perms?: string
  sort: number
  /** 后端 MenuSaveDTO.visible 为 Integer:1 显示 / 0 隐藏 */
  visible: number
}

export function getMenuTree() {
  return request<MenuVO[]>({ url: '/admin-api/system/menu/tree', method: 'get' })
}

export function createMenu(data: MenuForm) {
  return request<void>({ url: '/admin-api/system/menu', method: 'post', data })
}

export function updateMenu(id: string, data: MenuForm) {
  return request<void>({ url: `/admin-api/system/menu/${id}`, method: 'put', data })
}

export function deleteMenu(id: string) {
  return request<void>({ url: `/admin-api/system/menu/${id}`, method: 'delete' })
}

/* ---------------- 操作日志 ---------------- */

export interface OpLogVO {
  id: string
  adminId?: string
  adminName?: string
  operator?: string
  module?: string
  title?: string
  method?: string
  uri?: string
  ip?: string
  params?: string
  result?: string
  costMs?: number
  status?: number
  errorMsg?: string
  createdAt?: string
  createTime?: string
  description?: string
  path?: string
  resultCode?: number
}

export function getOplogPage(params: PageQuery) {
  return request<PageResult<OpLogVO>>({
    url: '/admin-api/system/oplog/page',
    method: 'get',
    params,
  })
}

export function getOplogDetail(id: string) {
  return request<OpLogVO>({ url: `/admin-api/system/oplog/${id}`, method: 'get' })
}
