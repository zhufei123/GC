<template>
  <view class="app-map-view">
    <!-- #ifdef H5 -->
    <view v-if="!failed" :id="mapId" class="app-map-view__canvas" />
    <view v-if="!ready && !failed" class="app-map-view__loading">
      <text>地图加载中…</text>
    </view>
    <!-- #endif -->

    <!-- #ifndef H5 -->
    <map
      class="app-map-view__canvas"
      :latitude="latitude"
      :longitude="longitude"
      :markers="nativeMarkers"
      show-location
      @markertap="onNativeMarkerTap"
    />
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue";
import type { MapProvider } from "@/utils/map-provider";
// #ifdef H5
import {
  resolveProvider,
  loadAmapScript,
  loadQqmapScript,
  wgs84ToGcj02,
} from "@/utils/map-provider";
// #endif

export interface MapMarkerItem {
  id: string | number;
  latitude: number;
  longitude: number;
  title?: string;
}

const props = withDefaults(
  defineProps<{
    latitude: number;
    longitude: number;
    markers?: MapMarkerItem[];
    selectedId?: string | number | null;
  }>(),
  {
    markers: () => [],
    selectedId: null,
  }
);

const emit = defineEmits<{
  (e: "marker-tap", marker: MapMarkerItem): void;
  (e: "ready", provider: MapProvider): void;
  (e: "fail"): void;
}>();

const ready = ref(false);
const failed = ref(false);
const mapId = `app-map-view-${Math.random().toString(36).slice(2, 10)}`;

const validMarkers = computed(() =>
  (props.markers || []).filter((m) => {
    const lat = Number(m.latitude);
    const lng = Number(m.longitude);
    return !!lat && !!lng && !Number.isNaN(lat) && !Number.isNaN(lng);
  })
);

function isSelected(marker: MapMarkerItem): boolean {
  return props.selectedId != null && String(props.selectedId) === String(marker.id);
}

/* ---------- 非 H5:原生 map 组件 ---------- */

const nativeMarkers = computed(() =>
  validMarkers.value.map((item, index) => ({
    id: index,
    latitude: Number(item.latitude),
    longitude: Number(item.longitude),
    title: item.title || "",
    iconPath: "",
    width: isSelected(item) ? 36 : 28,
    height: isSelected(item) ? 36 : 28,
    callout: item.title
      ? {
          content: item.title,
          display: "BYCLICK" as const,
          padding: 6,
          borderRadius: 6,
          fontSize: 12,
          color: "#1f2329",
          bgColor: "#ffffff",
        }
      : undefined,
  }))
);

function onNativeMarkerTap(e: any) {
  const idx = Number(e?.detail?.markerId ?? e?.markerId);
  const item = validMarkers.value[idx];
  if (item) emit("marker-tap", item);
}

/* ---------- H5:按提供商初始化(amap / qqmap / osm) ---------- */

// #ifdef H5

let provider: MapProvider = "osm";
let mapInst: any = null;
let sdk: any = null;
// 各提供商的覆盖物引用,同步 markers 时统一清理
let overlays: any[] = [];

function pinHtml(active: boolean): string {
  return `<div class="app-map-pin${active ? " app-map-pin--active" : ""}"><div class="app-map-pin__dot"></div></div>`;
}

function mapContainer(): HTMLElement {
  const el = document.getElementById(mapId);
  if (!el) throw new Error("map container missing");
  return el;
}

async function initAmap() {
  const AMap = await loadAmapScript();
  const c = wgs84ToGcj02(props.longitude, props.latitude);
  mapInst = new AMap.Map(mapContainer(), {
    zoom: 13,
    center: [c.longitude, c.latitude],
  });
  sdk = AMap;
}

async function initQqmap() {
  const TMap = await loadQqmapScript();
  const c = wgs84ToGcj02(props.longitude, props.latitude);
  mapInst = new TMap.Map(mapContainer(), {
    center: new TMap.LatLng(c.latitude, c.longitude),
    zoom: 13,
  });
  sdk = TMap;
}

async function initOsm() {
  await import("leaflet/dist/leaflet.css");
  const mod: any = await import("leaflet");
  const leaflet = mod.default || mod;
  mapInst = leaflet
    .map(mapContainer(), { zoomControl: false })
    .setView([props.latitude, props.longitude], 13);
  leaflet
    .tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "© OpenStreetMap",
    })
    .addTo(mapInst);
  sdk = leaflet;
}

async function initH5Map() {
  const preferred = resolveProvider();
  const chain: MapProvider[] = preferred === "osm" ? ["osm"] : [preferred, "osm"];
  for (const p of chain) {
    try {
      if (p === "amap") await initAmap();
      else if (p === "qqmap") await initQqmap();
      else await initOsm();
      provider = p;
      ready.value = true;
      emit("ready", p);
      syncMarkers();
      return;
    } catch (e) {
      destroyMap();
    }
  }
  // 全部失败:置空,父级列表不受影响
  failed.value = true;
  emit("fail");
}

function destroyMap() {
  overlays = [];
  if (!mapInst) {
    sdk = null;
    return;
  }
  try {
    if (typeof mapInst.destroy === "function") mapInst.destroy();
    else if (typeof mapInst.remove === "function") mapInst.remove();
  } catch (e) {
    /* ignore */
  }
  mapInst = null;
  sdk = null;
}

function syncMarkers() {
  if (!ready.value || !mapInst || !sdk) return;
  try {
    if (provider === "amap") syncAmapMarkers();
    else if (provider === "qqmap") syncQqmapMarkers();
    else syncOsmMarkers();
  } catch (e) {
    /* 标记渲染失败不阻塞页面 */
  }
}

function fitPoints(): Array<{ latitude: number; longitude: number }> {
  return [
    { latitude: props.latitude, longitude: props.longitude },
    ...validMarkers.value.map((m) => ({
      latitude: Number(m.latitude),
      longitude: Number(m.longitude),
    })),
  ];
}

function syncAmapMarkers() {
  if (overlays.length) {
    mapInst.remove(overlays);
    overlays = [];
  }
  const center = wgs84ToGcj02(props.longitude, props.latitude);
  const userMarker = new sdk.Marker({
    position: [center.longitude, center.latitude],
    content: '<div class="app-map-user-dot"></div>',
    offset: new sdk.Pixel(-10, -10),
    zIndex: 90,
  });
  overlays.push(userMarker);
  for (const item of validMarkers.value) {
    const g = wgs84ToGcj02(Number(item.longitude), Number(item.latitude));
    const active = isSelected(item);
    const marker = new sdk.Marker({
      position: [g.longitude, g.latitude],
      content: pinHtml(active),
      offset: new sdk.Pixel(-15, -40),
      title: item.title || "",
      zIndex: active ? 110 : 100,
    });
    marker.on("click", () => emit("marker-tap", item));
    overlays.push(marker);
  }
  mapInst.add(overlays);
  if (overlays.length > 1) {
    mapInst.setFitView(overlays, false, [30, 30, 30, 30], 15);
  } else {
    mapInst.setCenter([center.longitude, center.latitude]);
  }
}

function syncQqmapMarkers() {
  for (const o of overlays) {
    try {
      o.setMap(null);
    } catch (e) {
      /* ignore */
    }
  }
  overlays = [];
  const center = wgs84ToGcj02(props.longitude, props.latitude);
  // 用户位置:小圆点(米制半径,近似展示)
  const userCircle = new sdk.MultiCircle({
    map: mapInst,
    styles: {
      user: new sdk.CircleStyle({
        color: "rgba(77, 128, 240, 0.35)",
        borderColor: "#4d80f0",
        borderWidth: 2,
      }),
    },
    geometries: [
      {
        styleId: "user",
        center: new sdk.LatLng(center.latitude, center.longitude),
        radius: 40,
      },
    ],
  });
  overlays.push(userCircle);

  const geometries = validMarkers.value.map((item, index) => {
    const g = wgs84ToGcj02(Number(item.longitude), Number(item.latitude));
    return {
      id: String(index),
      styleId: isSelected(item) ? "active" : "store",
      position: new sdk.LatLng(g.latitude, g.longitude),
    };
  });
  const multiMarker = new sdk.MultiMarker({
    map: mapInst,
    styles: {
      store: new sdk.MarkerStyle({ width: 27, height: 35, anchor: { x: 13, y: 35 } }),
      active: new sdk.MarkerStyle({ width: 34, height: 44, anchor: { x: 17, y: 44 } }),
    },
    geometries,
  });
  multiMarker.on("click", (evt: any) => {
    const item = validMarkers.value[Number(evt?.geometry?.id)];
    if (item) emit("marker-tap", item);
  });
  overlays.push(multiMarker);

  const points = fitPoints();
  if (points.length > 1) {
    const bounds = new sdk.LatLngBounds();
    for (const p of points) {
      const g = wgs84ToGcj02(p.longitude, p.latitude);
      bounds.extend(new sdk.LatLng(g.latitude, g.longitude));
    }
    mapInst.fitBounds(bounds, { padding: 30 });
  } else {
    mapInst.setCenter(new sdk.LatLng(center.latitude, center.longitude));
  }
}

function syncOsmMarkers() {
  for (const o of overlays) {
    try {
      o.remove();
    } catch (e) {
      /* ignore */
    }
  }
  overlays = [];
  const userMarker = sdk
    .marker([props.latitude, props.longitude], {
      icon: sdk.divIcon({
        className: "",
        html: '<div class="app-map-user-dot"></div>',
        iconSize: [20, 20],
        iconAnchor: [10, 10],
      }),
    })
    .addTo(mapInst);
  overlays.push(userMarker);
  for (const item of validMarkers.value) {
    const marker = sdk
      .marker([Number(item.latitude), Number(item.longitude)], {
        icon: sdk.divIcon({
          className: "",
          html: pinHtml(isSelected(item)),
          iconSize: [30, 40],
          iconAnchor: [15, 40],
        }),
      })
      .on("click", () => emit("marker-tap", item))
      .addTo(mapInst);
    overlays.push(marker);
  }
  const points = fitPoints().map((p) => [p.latitude, p.longitude] as [number, number]);
  if (points.length > 1) {
    mapInst.fitBounds(points, { padding: [30, 30], maxZoom: 15 });
  } else {
    mapInst.setView([props.latitude, props.longitude], 13);
  }
}

// #endif

watch(
  () => [props.markers, props.latitude, props.longitude, props.selectedId],
  () => {
    // #ifdef H5
    syncMarkers();
    // #endif
  },
  { deep: true }
);

onMounted(() => {
  // #ifdef H5
  setTimeout(initH5Map, 0);
  // #endif
});

onBeforeUnmount(() => {
  // #ifdef H5
  destroyMap();
  // #endif
});
</script>

<!-- 地图标记为动态注入 DOM,样式不能 scoped(仅 H5 生效) -->
<style lang="scss">
.app-map-user-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #4d80f0;
  border: 3px solid #fff;
  box-shadow: 0 0 0 4px rgba(77, 128, 240, 0.25);
  box-sizing: border-box;
}

.app-map-pin {
  width: 30px;
  height: 30px;
  border-radius: 50% 50% 50% 0;
  background: #07c160;
  transform: rotate(-45deg);
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;

  &__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #fff;
  }

  &--active {
    background: #ff7d00;
    transform: rotate(-45deg) scale(1.2);
  }
}
</style>

<style lang="scss" scoped>
.app-map-view {
  position: relative;
  width: 100%;
  height: 100%;

  &__canvas {
    width: 100%;
    height: 100%;
  }

  &__loading {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #86909c;
    font-size: 24rpx;
    background: #f2f3f5;
    z-index: 500;
  }
}
</style>
