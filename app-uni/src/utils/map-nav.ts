/**
 * 打开外部导航/位置标注。
 * H5 按当前地图提供商跳转对应 URI(高德/腾讯 marker 页或 OSM),
 * 其余端调用 uni.openLocation 唤起系统地图能力。
 */
import { resolveProvider, wgs84ToGcj02 } from "./map-provider";

export interface NavTarget {
  latitude: number;
  longitude: number;
  name?: string;
  address?: string;
}

export function openNavigation(target: NavTarget): void {
  const lat = Number(target.latitude);
  const lng = Number(target.longitude);
  if (!lat || !lng || Number.isNaN(lat) || Number.isNaN(lng)) return;
  const name = target.name || "目的地";
  const address = target.address || "";

  // #ifdef H5
  const provider = resolveProvider();
  let url: string;
  if (provider === "amap") {
    // 高德 URI API 要求 GCJ-02 坐标,position 为 lng,lat
    const g = wgs84ToGcj02(lng, lat);
    url = `https://uri.amap.com/marker?position=${g.longitude},${g.latitude}&name=${encodeURIComponent(
      name
    )}&callnative=1`;
  } else if (provider === "qqmap") {
    // 腾讯 URI API 要求 GCJ-02 坐标,coord 为 lat,lng;referer 为必填
    const g = wgs84ToGcj02(lng, lat);
    const marker = `coord:${g.latitude},${g.longitude};title:${encodeURIComponent(
      name
    )};addr:${encodeURIComponent(address || name)}`;
    url = `https://apis.map.qq.com/uri/v1/marker?marker=${marker}&referer=app-uni`;
  } else {
    url = `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=17/${lat}/${lng}`;
  }
  window.open(url);
  // #endif

  // #ifndef H5
  uni.openLocation({
    latitude: lat,
    longitude: lng,
    name,
    address,
  });
  // #endif
}
