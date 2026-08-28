import request from './request'
import type { PageQuery, PageResult } from '@/types/api'

/* ---------------- 入驻审核 ---------------- */

export interface StoreApplyVO {
  id: string
  userId?: string
  storeName?: string
  name?: string
  contactName?: string
  applicant?: string
  phone?: string
  province?: string
  city?: string
  district?: string
  address?: string
  detail?: string
  images?: string[]
  licenseImages?: string[]
  status?: string | number
  remark?: string
  auditRemark?: string
  contactPhone?: string
  auditStatus?: string
  storeImages?: string | string[]
  createdAt?: string
  createTime?: string
  auditedAt?: string
}

export function getApplyPage(params: PageQuery) {
  return request<PageResult<StoreApplyVO>>({
    url: '/admin-api/store/apply/page',
    method: 'get',
    params,
  })
}

export function getApplyDetail(id: string) {
  return request<StoreApplyVO>({ url: `/admin-api/store/apply/${id}`, method: 'get' })
}

export function auditApply(id: string, data: { pass: boolean; remark?: string }) {
  return request<void>({ url: `/admin-api/store/apply/${id}/audit`, method: 'post', data })
}

/* ---------------- 门店 ---------------- */

export interface StoreVO {
  id: string
  name: string
  contactName?: string
  bossName?: string
  phone?: string
  province?: string
  city?: string
  district?: string
  address?: string
  detail?: string
  longitude?: number
  latitude?: number
  businessStatus?: string | number
  status?: number
  createdAt?: string
  createTime?: string
}

export interface StoreForm {
  name?: string
  contactName?: string
  phone?: string
  address?: string
  [key: string]: unknown
}

export function getStorePage(params: PageQuery) {
  return request<PageResult<StoreVO>>({ url: '/admin-api/store/page', method: 'get', params })
}

export function updateStore(id: string, data: StoreForm) {
  return request<void>({ url: `/admin-api/store/${id}`, method: 'put', data })
}

export function updateStoreStatus(id: string, status: number) {
  return request<void>({ url: `/admin-api/store/${id}/status`, method: 'put', data: { status } })
}
