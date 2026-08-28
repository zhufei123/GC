import request from './request'
import type { MenuVO } from '@/types/menu'

export interface LoginParams {
  username: string
  password: string
}

export interface AdminInfo {
  id: string
  username: string
  nickname: string
  avatar?: string
  phone?: string
}

export interface LoginResult {
  token: string
  admin: AdminInfo
  roles: string[]
  perms: string[]
}

/** /auth/me 兼容 { admin, roles, perms } 与平铺两种结构 */
export interface MeResult extends Partial<AdminInfo> {
  admin?: AdminInfo
  roles?: string[]
  perms?: string[]
}

export function login(data: LoginParams) {
  return request<LoginResult>({ url: '/admin-api/auth/login', method: 'post', data })
}

export function logout() {
  return request<void>({ url: '/admin-api/auth/logout', method: 'post' })
}

export function getMe() {
  return request<MeResult>({ url: '/admin-api/auth/me', method: 'get' })
}

export function getMenus() {
  return request<MenuVO[]>({ url: '/admin-api/auth/menus', method: 'get' })
}
