import { get, post } from "@/utils/request";
import type { PageResult } from "@/utils/request";

export interface EstimateItem {
  skuId: string;
  estimateWeight: string;
}

export interface CreateOrderData {
  type: "PICKUP" | "DROPOFF";
  addressId?: string;
  storeId?: string;
  appointDate: string;
  appointPeriod: string;
  estimateItems: EstimateItem[];
  images: string[];
  remark?: string;
  requestId: string;
}

export interface OrderVO {
  id: string;
  orderNo?: string;
  status: string;
  type?: string;
  /** 回收站 id/名称(再来一单用) */
  stationId?: string | number;
  stationName?: string;
  address?: string;
  receiver?: string;
  phone?: string;
  /** 上门地址坐标(后端返回时用于导航) */
  longitude?: number | string;
  latitude?: number | string;
  appointDate?: string;
  appointPeriod?: string;
  estimateAmount?: string;
  actualAmount?: string;
  /** OFFLINE/WX_TRANSFER/ALIPAY_TRANSFER/WALLET */
  payMethod?: string;
  /** SUCCESS/PROCESSING/WAIT_USER_CONFIRM/FAILED */
  payoutStatus?: string;
  paidAt?: string;
  /** 微信商家转账确认收款 package 信息(详情返回) */
  packageInfo?: string;
  createTime?: string;
  /** 订单进度节点时间 yyyy-MM-dd HH:mm:ss */
  acceptedAt?: string;
  servedAt?: string;
  weighedAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  estimateItems?: OrderLineItem[];
  actualItems?: OrderLineItem[];
  items?: OrderLineItem[];
  /** 用户下单照片 */
  images?: string[];
  /** 称重现场照片 */
  weighImages?: string[];
  remark?: string;
  cancelReason?: string;
}

export interface OrderLineItem {
  skuId: string;
  skuName?: string;
  unit?: string;
  estimateWeight?: string;
  weight?: string;
  price?: string;
  amount?: string;
}

/** 兼容后端 items / estimateItems / actualItems */
export function orderLineItems(order?: OrderVO | null): OrderLineItem[] {
  if (!order) return [];
  if (order.items?.length) return order.items;
  if (order.actualItems?.length) return order.actualItems;
  return order.estimateItems || [];
}

export function createOrder(data: CreateOrderData) {
  return post<{ id: string }>("/app-api/order", data, { loading: true });
}

export function getOrderPage(params: { pageNum?: number; pageSize?: number; status?: string }) {
  const status = params.status && params.status !== "ALL" ? params.status : undefined;
  return get<PageResult<OrderVO>>(
    "/app-api/order/page",
    { pageNum: params.pageNum, pageSize: params.pageSize, ...(status ? { status } : {}) },
    { silent: true }
  );
}

export function getOrderDetail(id: string) {
  return get<OrderVO>(`/app-api/order/${id}`);
}

export function cancelOrder(id: string, reason: string) {
  return post(`/app-api/order/${id}/cancel`, { reason });
}

export interface TimeslotVO {
  date?: string;
  dateLabel?: string;
  periods?: string[];
  period?: string;
  value?: string;
}

export function getTimeslots() {
  return get<string[] | TimeslotVO[]>("/app-api/timeslots", undefined, { silent: true });
}

/* ---------- 订单评价（仅 COMPLETED，一单一评） ---------- */

export interface OrderReviewVO {
  orderId?: string;
  rating: number;
  comment?: string;
  createTime?: string;
}

export function submitOrderReview(id: string, rating: number, comment?: string) {
  return post<void>(`/app-api/order/${id}/review`, { rating, comment }, { loading: true });
}

/** 未评价返回 null */
export function getOrderReview(id: string) {
  return get<OrderReviewVO | null>(`/app-api/order/${id}/review`, undefined, { silent: true });
}

/** H5/mock 环境：微信商家转账「确认收款」直接确认(真机走 wx.requestMerchantTransfer) */
export function confirmWxPayout(id: string) {
  return post<void>(`/app-api/pay/wx-confirm/${id}`, {}, { loading: true });
}
