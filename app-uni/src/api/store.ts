import { get } from "@/utils/request";
import { getAddressList } from "@/api/address";

/** 深圳南山演示坐标(经度, 纬度) */
export const FALLBACK_LOCATION = { longitude: 113.953, latitude: 22.537 };

export interface StorePriceItem {
  skuId: string;
  skuName?: string;
  name?: string;
  price?: string | number | null;
  unit?: string;
  /** true 表示平台指导价(非门店报价) */
  guide?: boolean;
}

export interface StoreItem {
  id: string;
  name: string;
  address?: string;
  phone?: string;
  businessHours?: string;
  businessStatus?: number;
  longitude?: number;
  latitude?: number;
  distanceKm?: number;
  categoryIds?: string[];
  photos?: string[];
  /** 后端可能随 nearby 返回的高亮价格 */
  prices?: StorePriceItem[];
}

export interface SkuQuoteItem {
  storeId: string;
  storeName?: string;
  name?: string;
  address?: string;
  distanceKm?: number;
  price?: string | number | null;
  unit?: string;
  businessHours?: string;
  guide?: boolean;
}

export function getNearbyStores(longitude: number, latitude: number) {
  return get<StoreItem[]>("/app-api/store/nearby", { longitude, latitude }, { silent: true });
}

export function getStoreDetail(id: string) {
  return get<StoreItem>(`/app-api/store/${id}`, undefined, { silent: true });
}

export function getStorePrices(id: string) {
  return get<StorePriceItem[]>(`/app-api/store/${id}/prices`, undefined, { silent: true });
}

export function getSkuQuotes(skuId: string, longitude: number, latitude: number) {
  return get<SkuQuoteItem[]>(
    `/app-api/recycle/sku/${skuId}/quotes`,
    { longitude, latitude },
    { silent: true }
  );
}

/* ---------- nearby 结果缓存：detail 页在 /app-api/store/{id} 未就绪时兜底 ---------- */

let nearbyCache: StoreItem[] = [];

export function cacheNearbyStores(list: StoreItem[]) {
  nearbyCache = list || [];
}

export function getCachedStore(id: string): StoreItem | null {
  return nearbyCache.find((s) => String(s.id) === String(id)) || null;
}

/**
 * 解析用户坐标：默认地址 -> 第一个地址 -> 南山演示坐标。
 * 接口失败也返回兜底坐标，页面不因此中断。
 */
export async function resolveUserLocation(): Promise<{ longitude: number; latitude: number }> {
  try {
    const list = (await getAddressList()) || [];
    const preferred =
      list.find((a) => a.isDefault === true || Number(a.isDefault) === 1) || list[0];
    const lng = Number(preferred?.longitude);
    const lat = Number(preferred?.latitude);
    if (lng && lat && !Number.isNaN(lng) && !Number.isNaN(lat)) {
      return { longitude: lng, latitude: lat };
    }
  } catch (e) {
    /* 地址不可用时使用兜底坐标 */
  }
  return { ...FALLBACK_LOCATION };
}
