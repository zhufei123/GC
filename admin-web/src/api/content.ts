import request from './request'
import type { PageQuery, PageResult } from '@/types/api'

/* ---------------- Banner ---------------- */

/** 跳转类型:NONE 不跳转 / PAGE 页面路径 / RICH 富文本 */
export type BannerLinkType = 'NONE' | 'PAGE' | 'RICH'

export interface BannerVO {
  id: string
  title?: string
  image?: string
  linkType?: BannerLinkType | string
  linkUrl?: string
  sort?: number
  status?: number
  startTime?: string
  endTime?: string
  createdAt?: string
}

export interface BannerForm {
  title: string
  image: string
  linkType: BannerLinkType | string
  linkUrl?: string
  sort: number
  status: number
}

export function getBannerPage(params: PageQuery) {
  return request<PageResult<BannerVO>>({
    url: '/admin-api/content/banner/page',
    method: 'get',
    params,
  })
}

export function createBanner(data: BannerForm) {
  return request<void>({ url: '/admin-api/content/banner', method: 'post', data })
}

export function updateBanner(id: string, data: BannerForm) {
  return request<void>({ url: `/admin-api/content/banner/${id}`, method: 'put', data })
}

export function deleteBanner(id: string) {
  return request<void>({ url: `/admin-api/content/banner/${id}`, method: 'delete' })
}

/* ---------------- 公告 ---------------- */

export interface NoticeVO {
  id: string
  title?: string
  content?: string
  status?: number
  sort?: number
  pinned?: number
  publishStatus?: string
  publishedAt?: string
  publishTime?: string
  createdAt?: string
  createTime?: string
}

export interface NoticeForm {
  title: string
  content: string
  pinned: number
  status: number
  publishStatus?: string
}

export function getNoticePage(params: PageQuery) {
  return request<PageResult<NoticeVO>>({
    url: '/admin-api/content/notice/page',
    method: 'get',
    params,
  })
}

export function createNotice(data: NoticeForm) {
  return request<void>({ url: '/admin-api/content/notice', method: 'post', data })
}

export function updateNotice(id: string, data: NoticeForm) {
  return request<void>({ url: `/admin-api/content/notice/${id}`, method: 'put', data })
}

export function deleteNotice(id: string) {
  return request<void>({ url: `/admin-api/content/notice/${id}`, method: 'delete' })
}

/* ---------------- 评价审核 ---------------- */

export type ReviewAuditStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface OrderReviewAdminVO {
  id: string
  orderId?: string
  userId?: string
  stationId?: string
  rating?: number
  comment?: string
  auditStatus?: ReviewAuditStatus | string
  auditRemark?: string
  auditedAt?: string
  createTime?: string
}

export const REVIEW_AUDIT_MAP: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'danger' },
}

export function getReviewPage(params: PageQuery) {
  return request<PageResult<OrderReviewAdminVO>>({
    url: '/admin-api/content/review/page',
    method: 'get',
    params,
  })
}

export function auditReview(id: string, status: ReviewAuditStatus, remark?: string) {
  return request<void>({
    url: `/admin-api/content/review/${id}/audit`,
    method: 'post',
    data: { status, remark },
  })
}
