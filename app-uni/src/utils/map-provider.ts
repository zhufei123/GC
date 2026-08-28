/**
 * 地图提供商解析与 H5 SDK 加载。
 * 提供商由 VITE_MAP_PROVIDER 指定(默认 amap),所选提供商 key 为空时
 * 按 amap → qqmap → osm 顺序降级;osm 无需 key,永远可用。
 */

export type MapProvider = "amap" | "qqmap" | "osm";

export const AMAP_KEY = String(import.meta.env.VITE_AMAP_KEY || "").trim();
export const AMAP_SECURITY_CODE = String(import.meta.env.VITE_AMAP_SECURITY_CODE || "").trim();
export const QQMAP_KEY = String(import.meta.env.VITE_MAP_QQ_KEY || "").trim();

const FALLBACK_CHAIN: MapProvider[] = ["amap", "qqmap", "osm"];

function providerUsable(provider: MapProvider): boolean {
  if (provider === "amap") return !!AMAP_KEY;
  if (provider === "qqmap") return !!QQMAP_KEY;
  return true;
}

export function resolveProvider(): MapProvider {
  const preferred = String(import.meta.env.VITE_MAP_PROVIDER || "amap")
    .trim()
    .toLowerCase() as MapProvider;
  const start = FALLBACK_CHAIN.indexOf(preferred);
  const chain = start >= 0 ? FALLBACK_CHAIN.slice(start) : FALLBACK_CHAIN;
  for (const p of chain) {
    if (providerUsable(p)) return p;
  }
  return "osm";
}

/* ---------- H5 SDK 脚本加载(仅在 H5 条件编译代码中调用) ---------- */

// #ifdef H5

function injectScript(src: string, check: () => any): Promise<any> {
  return new Promise((resolve, reject) => {
    const existing = check();
    if (existing) {
      resolve(existing);
      return;
    }
    const script = document.createElement("script");
    script.src = src;
    script.async = true;
    script.onload = () => {
      const sdk = check();
      if (sdk) resolve(sdk);
      else reject(new Error(`sdk unavailable after load: ${src}`));
    };
    script.onerror = () => reject(new Error(`script load failed: ${src}`));
    document.head.appendChild(script);
  });
}

let amapPromise: Promise<any> | null = null;

/** 加载高德 JS API 2.0,resolve window.AMap */
export function loadAmapScript(): Promise<any> {
  if (!AMAP_KEY) return Promise.reject(new Error("amap key missing"));
  if (!amapPromise) {
    // 安全密钥须在脚本加载前挂到 window(高德 2.0 要求)
    if (AMAP_SECURITY_CODE) {
      (window as any)._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE };
    }
    amapPromise = injectScript(
      `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(AMAP_KEY)}`,
      () => (window as any).AMap
    ).catch((e) => {
      amapPromise = null;
      throw e;
    });
  }
  return amapPromise;
}

let qqmapPromise: Promise<any> | null = null;

/** 加载腾讯地图 GL JS,resolve window.TMap */
export function loadQqmapScript(): Promise<any> {
  if (!QQMAP_KEY) return Promise.reject(new Error("qqmap key missing"));
  if (!qqmapPromise) {
    qqmapPromise = injectScript(
      `https://map.qq.com/api/gljs?v=1.exp&key=${encodeURIComponent(QQMAP_KEY)}`,
      () => (window as any).TMap
    ).catch((e) => {
      qqmapPromise = null;
      throw e;
    });
  }
  return qqmapPromise;
}

// #endif

/* ---------- WGS-84 → GCJ-02(高德/腾讯坐标系)简易偏移公式 ---------- */

const PI = Math.PI;
const EARTH_A = 6378245.0;
const EARTH_EE = 0.00669342162296594323;

function outOfChina(lng: number, lat: number): boolean {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
}

function transformLat(x: number, y: number): number {
  let ret =
    -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0;
  ret += ((20.0 * Math.sin(y * PI) + 40.0 * Math.sin((y / 3.0) * PI)) * 2.0) / 3.0;
  ret += ((160.0 * Math.sin((y / 12.0) * PI) + 320.0 * Math.sin((y * PI) / 30.0)) * 2.0) / 3.0;
  return ret;
}

function transformLng(x: number, y: number): number {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0;
  ret += ((20.0 * Math.sin(x * PI) + 40.0 * Math.sin((x / 3.0) * PI)) * 2.0) / 3.0;
  ret += ((150.0 * Math.sin((x / 12.0) * PI) + 300.0 * Math.sin((x / 30.0) * PI)) * 2.0) / 3.0;
  return ret;
}

/** WGS-84 转 GCJ-02;境外坐标原样返回 */
export function wgs84ToGcj02(
  longitude: number,
  latitude: number
): { longitude: number; latitude: number } {
  const lng = Number(longitude);
  const lat = Number(latitude);
  if (Number.isNaN(lng) || Number.isNaN(lat) || outOfChina(lng, lat)) {
    return { longitude: lng, latitude: lat };
  }
  let dLat = transformLat(lng - 105.0, lat - 35.0);
  let dLng = transformLng(lng - 105.0, lat - 35.0);
  const radLat = (lat / 180.0) * PI;
  let magic = Math.sin(radLat);
  magic = 1 - EARTH_EE * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / (((EARTH_A * (1 - EARTH_EE)) / (magic * sqrtMagic)) * PI);
  dLng = (dLng * 180.0) / ((EARTH_A / sqrtMagic) * Math.cos(radLat) * PI);
  return { longitude: lng + dLng, latitude: lat + dLat };
}
