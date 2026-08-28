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
  address?: string;
  receiver?: string;
  phone?: string;
  appointDate?: string;
  appointPeriod?: string;
  estimateAmount?: string;
  actualAmount?: string;
  createTime?: string;
  estimateItems?: OrderLineItem[];
  actualItems?: OrderLineItem[];
  items?: OrderLineItem[];
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
  return get<PageResult<OrderVO>>("/app-api/order/page", params, { silent: true });
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
