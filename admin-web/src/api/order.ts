import request from './request'
import type { PageQuery, PageResult } from '@/types/api'

export type OrderStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'SERVING'
  | 'WEIGHED'
  | 'COMPLETED'
  | 'CANCELLED'

export type OrderTagType = 'success' | 'warning' | 'info' | 'danger' | 'primary'

/** 订单状态文案映射(docs/01) */
export const ORDER_STATUS_MAP: Record<string, { label: string; type: OrderTagType }> = {
  PENDING: { label: '待接单', type: 'warning' },
  ACCEPTED: { label: '已接单', type: 'primary' },
  SERVING: { label: '服务中', type: 'primary' },
  WEIGHED: { label: '已称重', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
}

export function orderStatusInfo(status?: string): { label: string; type: OrderTagType } {
  return ORDER_STATUS_MAP[status ?? ''] ?? { label: status || '-', type: 'info' }
}

/** 订单类型文案 */
export const ORDER_TYPE_MAP: Record<string, string> = {
  PICKUP: '上门回收',
  DROPOFF: '到店回收',
}

export interface OrderItemVO {
  skuId?: string
  skuName?: string
  unit?: string
  price?: string
  estimateWeight?: string
  weight?: string
  amount?: string
}

export interface OrderTimelineVO {
  status?: string
  /** 后端 AdminOrderVO.TimelineVO 的节点文案字段 */
  label?: string
  title?: string
  remark?: string
  time?: string
  createdAt?: string
}

/** 对齐后端 AdminOrderVO */
export interface OrderVO {
  id: string
  orderNo?: string
  type?: string
  status?: OrderStatus | string
  userId?: string
  /** 联系人(下单地址快照) */
  receiver?: string
  /** 联系电话(列表脱敏,详情明文) */
  phone?: string
  stationId?: string
  stationName?: string
  appointDate?: string
  appointPeriod?: string
  estimateAmount?: string
  /** 实收金额(称重确认后) */
  actualAmount?: string
  address?: string
  addressSnapshot?: Record<string, unknown> | string
  estimateItems?: OrderItemVO[]
  actualItems?: OrderItemVO[]
  images?: string[]
  weighImages?: string[]
  remark?: string
  cancelBy?: string
  cancelReason?: string
  timeline?: OrderTimelineVO[]
  createdAt?: string
  createTime?: string
  completedAt?: string
  cancelledAt?: string
}

export function getOrderPage(params: PageQuery) {
  return request<PageResult<OrderVO>>({
    url: '/admin-api/trade/order/page',
    method: 'get',
    params,
  })
}

export function getOrderDetail(id: string) {
  return request<OrderVO>({ url: `/admin-api/trade/order/${id}`, method: 'get' })
}

export function cancelOrder(id: string, reason: string) {
  return request<void>({
    url: `/admin-api/trade/order/${id}/cancel`,
    method: 'post',
    data: { reason },
  })
}
