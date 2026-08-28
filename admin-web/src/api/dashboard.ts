import request from './request'

export interface DashboardSummary {
  todayOrderCount: number
  todayWeightKg: string
  todayAmount: string
  totalUserCount: number
  totalStoreCount: number
  pendingApplyCount: number
}

export function getDashboardSummary() {
  return request<DashboardSummary>({ url: '/admin-api/dashboard/summary', method: 'get' })
}

export function getDashboardTrend(params?: Record<string, unknown>) {
  return request<unknown>({ url: '/admin-api/dashboard/trend', method: 'get', params })
}

export function getCategoryRank(params?: Record<string, unknown>) {
  return request<unknown>({ url: '/admin-api/dashboard/category-rank', method: 'get', params })
}
