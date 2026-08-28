import request from './request'
import type { PageQuery, PageResult } from '@/types/api'

export interface MemberUserVO {
  id: string
  nickname?: string
  avatar?: string
  phone?: string
  role?: string
  recyclerStatus?: string
  status?: number
  orderCount?: number
  createdAt?: string
  lastLoginAt?: string
}

export function getUserPage(params: PageQuery) {
  return request<PageResult<MemberUserVO>>({
    url: '/admin-api/member/user/page',
    method: 'get',
    params,
  })
}

export function getUserDetail(id: string) {
  return request<MemberUserVO>({ url: `/admin-api/member/user/${id}`, method: 'get' })
}

export function updateUserStatus(id: string, status: number) {
  return request<void>({
    url: `/admin-api/member/user/${id}/status`,
    method: 'put',
    data: { status },
  })
}
