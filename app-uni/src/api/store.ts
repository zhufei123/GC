import { get } from "@/utils/request";
import { getAddressList } from "@/api/address";

/** 深圳南山演示坐标(经度, 纬度) */
export const FALLBACK_LOCATION = { longitude: 113.953, latitude: 22.537 };

/** 门店报价单条目(/store/{id}/prices)，guide=true 表示平台指导价兜底 */
export interface StorePriceItem {
  skuId: string;
  skuName?: string;
  name?: string;
  price?: string | number | null;
  unit?: string;
  categoryName?: string;
  /** 1报价中 0停报 */
  status?: number;
  /** 平台指导价(门店报价对比用) */
  guidePrice?: string | number | null;
  /** true 表示该条为平台指导价(非门店报价) */
  guide?: boolean;
  /** 报价最后更新时间 yyyy-MM-dd HH:mm:ss */
  updatedAt?: string;
}

/** nearby 返回的 TOP 报价简项 */
export interface PriceBrief {
  skuName?: string;
  price?: string | number | null;
  /** 报价最后更新时间 yyyy-MM-dd HH:mm:ss */
  updatedAt?: string;
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
  /** 亮点价(纸类/塑料最高报价) */
  highlightPrice?: string | number | null;
  /** 报价中 SKU 数 */
  quotedCount?: number;
  /** 营业中且处于营业时段 */
  openNow?: boolean;
  /** 报价 TOP3(按价格降序) */
  prices?: PriceBrief[];
}

export interface SkuQuoteItem {
  storeId: string;
  storeName?: string;
  address?: string;
  longitude?: number;
  latitude?: number;
  distanceKm?: number;
  price?: string | number | null;
  unit?: string;
  businessHours?: string;
  businessStatus?: number;
  openNow?: boolean;
  /** true 表示该条为平台指导价(非门店报价) */
  guide?: boolean;
  /** 报价最后更新时间 yyyy-MM-dd HH:mm:ss */
  priceUpdatedAt?: string;
}

export function getNearbyStores(longitude: number, latitude: number, extra?: { radiusKm?: number; sort?: string }) {
  return get<StoreItem[]>(
    "/app-api/store/nearby",
    { longitude, latitude, radiusKm: extra?.radiusKm ?? 20, sort: extra?.sort },
    { silent: true }
  );
}

export function getStoreDetail(id: string, longitude?: number, latitude?: number) {
  return get<StoreItem>(
    `/app-api/store/${id}`,
    longitude != null && latitude != null ? { longitude, latitude } : undefined,
    { silent: true }
  );
}

export function getStorePrices(id: string) {
  return get<StorePriceItem[]>(`/app-api/store/${id}/prices`, undefined, { silent: true });
}

export async function getSkuQuotes(
  skuId: string,
  longitude: number,
  latitude: number
): Promise<SkuQuoteItem[]> {
  const list = await get<any[]>(
    `/app-api/recycle/sku/${skuId}/quotes`,
    { longitude, latitude },
    { silent: true }
  );
  // 后端字段为 stationId/stationName/updatedAt，统一映射为 storeId/storeName/priceUpdatedAt
  return (list || []).map((q) => ({
    ...q,
    storeId: String(q.storeId ?? q.stationId ?? ""),
    storeName: q.storeName ?? q.stationName,
    priceUpdatedAt: q.priceUpdatedAt ?? q.updatedAt,
  }));
}

/* ---------- nearby 结果缓存：detail 页在 /app-api/store/{id} 未就绪时兜底 ---------- */

let nearbyCache: StoreItem[] = [];

export function cacheNearbyStores(list: StoreItem[]) {
  nearbyCache = list || [];
}

export function getCachedStore(id: string): StoreItem | null {
  return nearbyCache.find((s) => String(s.id) === String(id)) || null;
}

/** 设备定位(静默失败;超时视为失败,避免授权弹窗阻塞页面加载) */
function getDeviceLocation(
  timeoutMs = 3000
): Promise<{ longitude: number; latitude: number } | null> {
  return new Promise((resolve) => {
    let settled = false;
    const finish = (v: { longitude: number; latitude: number } | null) => {
      if (settled) return;
      settled = true;
      resolve(v);
    };
    const timer = setTimeout(() => finish(null), timeoutMs);
    try {
      uni.getLocation({
        type: "wgs84",
        success: (res) => {
          clearTimeout(timer);
          const lng = Number(res?.longitude);
          const lat = Number(res?.latitude);
          if (lng && lat && !Number.isNaN(lng) && !Number.isNaN(lat)) {
            finish({ longitude: lng, latitude: lat });
          } else {
            finish(null);
          }
        },
        fail: () => {
          clearTimeout(timer);
          finish(null);
        },
      });
    } catch (e) {
      clearTimeout(timer);
      finish(null);
    }
  });
}

/**
 * 解析用户坐标：设备定位 -> 默认地址 -> 第一个地址 -> 南山演示坐标。
 * 任一环节失败均静默降级，页面不因此中断。
 */
export async function resolveUserLocation(): Promise<{ longitude: number; latitude: number }> {
  const device = await getDeviceLocation();
  if (device) return device;
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
