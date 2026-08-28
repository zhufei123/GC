import request from './request'
import type { PageQuery, PageResult } from '@/types/api'

export type PayoutChannel = 'OFFLINE' | 'WX_TRANSFER' | 'ALIPAY_TRANSFER' | 'WALLET'

export type PayoutStatus = 'PROCESSING' | 'WAIT_USER_CONFIRM' | 'SUCCESS' | 'FAILED'

export type PayoutTagType = 'success' | 'warning' | 'info' | 'danger' | 'primary'

/** 打款渠道文案映射 */
export const PAYOUT_CHANNEL_MAP: Record<string, string> = {
  OFFLINE: '线下现金',
  WX_TRANSFER: '微信打款',
  ALIPAY_TRANSFER: '支付宝打款',
  WALLET: '平台钱包',
}

export function payoutChannelText(channel?: string): string {
  return PAYOUT_CHANNEL_MAP[channel ?? ''] || channel || '-'
}

/** 打款状态文案映射 */
export const PAYOUT_STATUS_MAP: Record<string, { label: string; type: PayoutTagType }> = {
  PROCESSING: { label: '处理中', type: 'primary' },
  WAIT_USER_CONFIRM: { label: '待用户确认', type: 'warning' },
  SUCCESS: { label: '成功', type: 'success' },
  FAILED: { label: '失败', type: 'danger' },
}

export function payoutStatusInfo(status?: string): { label: string; type: PayoutTagType } {
  return PAYOUT_STATUS_MAP[status ?? ''] ?? { label: status || '-', type: 'info' }
}

/** 对齐后端 PayoutOrder */
export interface PayoutOrderVO {
  id: string
  payoutNo?: string
  orderId?: string
  userId?: string
  stationId?: string
  channel?: PayoutChannel | string
  amount?: string
  status?: PayoutStatus | string
  /** 转账收款方 openid(WX/ALIPAY 渠道) */
  openid?: string
  channelBillNo?: string
  /** 微信商家转账用户确认收款 package 信息 */
  packageInfo?: string
  failReason?: string
  createTime?: string
}

export function getPayoutPage(params: PageQuery) {
  return request<PageResult<PayoutOrderVO>>({
    url: '/admin-api/finance/payout/page',
    method: 'get',
    params,
  })
}
