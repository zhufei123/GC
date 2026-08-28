import { get, post, put } from "@/utils/request";
import type { PageResult } from "@/utils/request";
import type { OrderVO } from "@/api/order";

export interface WorkbenchData {
  todayOrderCount?: number;
  todayWeightKg?: string;
  todayAmount?: string;
  pendingCount?: number;
  servingCount?: number;
  poolCount?: number;
  businessStatus?: number;
  storeName?: string;
  [key: string]: any;
}

export function getWorkbench() {
  return get<WorkbenchData>("/app-api/boss/workbench", undefined, { silent: true });
}

export function updateBusinessStatus(businessStatus: number) {
  return put("/app-api/boss/store/business-status", { businessStatus });
}

export function getOrderPool(params?: { pageNum?: number; pageSize?: number }) {
  return get<PageResult<OrderVO>>("/app-api/boss/order/pool", params, { silent: true });
}

export function acceptOrder(id: string) {
  return post(`/app-api/boss/order/${id}/accept`, {}, { loading: true });
}

export function getBossOrderPage(params: { pageNum?: number; pageSize?: number; status?: string }) {
  return get<PageResult<OrderVO>>("/app-api/boss/order/page", params, { silent: true });
}

export function getBossOrderDetail(id: string) {
  return get<OrderVO>(`/app-api/boss/order/${id}`);
}

export function startService(id: string) {
  return post(`/app-api/boss/order/${id}/start`, {}, { loading: true });
}

export function getWeighInit(id: string) {
  return get<OrderVO>(`/app-api/boss/order/${id}/weigh-init`, undefined, { silent: true });
}

export function getAvailableSkus() {
  return get<Array<{ id: string; name: string; unit?: string; price?: string }>>(
    "/app-api/boss/skus/available",
    undefined,
    { silent: true }
  );
}

export function submitWeigh(
  id: string,
  data: { items: Array<{ skuId: string; weight: string }>; images: string[]; remark?: string }
) {
  return post(`/app-api/boss/order/${id}/weigh`, data, { loading: true });
}

export function completeOrder(id: string, confirmAmount: string) {
  return post(`/app-api/boss/order/${id}/complete`, { confirmAmount }, { loading: true });
}

export function getBossStore() {
  return get("/app-api/boss/store", undefined, { silent: true });
}

export interface StoreApplyData {
  storeName: string;
  contactName: string;
  contactPhone: string;
  province?: string;
  city?: string;
  district?: string;
  detail: string;
  longitude?: number;
  latitude?: number;
  licenseImage?: string;
  storeImages?: string[];
  categoryIds?: string[];
}

export function applyStore(data: StoreApplyData) {
  return post("/app-api/store/apply", data, { loading: true });
}

export interface ApplyVO extends StoreApplyData {
  id: string;
  auditStatus: "pending" | "approved" | "rejected";
  auditRemark?: string;
  createTime?: string;
}

export function getLatestApply() {
  return get<ApplyVO | null>("/app-api/store/apply/latest", undefined, { silent: true });
}
