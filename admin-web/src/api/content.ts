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
