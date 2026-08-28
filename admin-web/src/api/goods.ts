import request from './request'
import type { PageQuery, PageResult } from '@/types/api'

/* ---------------- 分类 ---------------- */

export interface CategoryVO {
  id: string
  parentId: string
  name: string
  icon?: string
  sort: number
  status: number
  children?: CategoryVO[]
  createdAt?: string
}

export interface CategoryForm {
  parentId: string
  name: string
  icon?: string
  sort: number
  status: number
}

export function getCategoryTree() {
  return request<CategoryVO[]>({ url: '/admin-api/recycle/category/tree', method: 'get' })
}

export function createCategory(data: CategoryForm) {
  return request<void>({ url: '/admin-api/recycle/category', method: 'post', data })
}

export function updateCategory(id: string, data: CategoryForm) {
  return request<void>({ url: `/admin-api/recycle/category/${id}`, method: 'put', data })
}

export function deleteCategory(id: string) {
  return request<void>({ url: `/admin-api/recycle/category/${id}`, method: 'delete' })
}

export function updateCategoryStatus(id: string, status: number) {
  return request<void>({
    url: `/admin-api/recycle/category/${id}/status`,
    method: 'put',
    data: { status },
  })
}

/* ---------------- SKU ---------------- */

export interface SkuVO {
  id: string
  categoryId: string
  categoryName?: string
  name: string
  image?: string
  unit: string
  price?: string
  sort: number
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface SkuForm {
  categoryId: string
  name: string
  image?: string
  unit: string
  price?: string
  sort: number
  status: number
}

export function getSkuPage(params: PageQuery) {
  return request<PageResult<SkuVO>>({ url: '/admin-api/recycle/sku/page', method: 'get', params })
}

export function getSkuDetail(id: string) {
  return request<SkuVO>({ url: `/admin-api/recycle/sku/${id}`, method: 'get' })
}

export function createSku(data: SkuForm) {
  return request<void>({ url: '/admin-api/recycle/sku', method: 'post', data })
}

export function updateSku(id: string, data: SkuForm) {
  return request<void>({ url: `/admin-api/recycle/sku/${id}`, method: 'put', data })
}

export function deleteSku(id: string) {
  return request<void>({ url: `/admin-api/recycle/sku/${id}`, method: 'delete' })
}

export function updateSkuStatus(id: string, status: number) {
  return request<void>({
    url: `/admin-api/recycle/sku/${id}/status`,
    method: 'put',
    data: { status },
  })
}

/* ---------------- 价格 ---------------- */

export interface PriceForm {
  price: string
  effectiveAt?: string
  reason?: string
}

export interface PriceLogVO {
  id: string
  skuId?: string
  skuName?: string
  oldPrice?: string
  newPrice?: string
  price?: string
  effectiveAt?: string
  reason?: string
  operator?: string
  operatorName?: string
  createdAt?: string
}

export function updateSkuPrice(id: string, data: PriceForm) {
  return request<void>({ url: `/admin-api/recycle/sku/${id}/price`, method: 'put', data })
}

export function batchUpdateSkuPrice(data: { skuIds: string[]; price: string; reason?: string }) {
  return request<void>({ url: '/admin-api/recycle/sku/price/batch', method: 'put', data })
}

export function getSkuPriceLog(id: string, params?: PageQuery) {
  return request<PageResult<PriceLogVO> | PriceLogVO[]>({
    url: `/admin-api/recycle/sku/${id}/price-log`,
    method: 'get',
    params,
  })
}
