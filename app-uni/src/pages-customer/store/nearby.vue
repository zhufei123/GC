<template>
  <view class="nearby">
    <!-- H5 地图（加载失败时仅展示列表） -->
    <!-- #ifdef H5 -->
    <view v-if="!mapFailed" class="nearby__map-wrap">
      <view id="nearby-map" class="nearby__map" />
      <view v-if="!mapReady" class="nearby__map-loading">
        <wd-loading color="#07c160" />
        <text>地图加载中…</text>
      </view>
    </view>
    <!-- #endif -->

    <!-- 选中站点卡片(点击地图标记) -->
    <view v-if="selectedStore" class="picked card" @tap="goDetail(selectedStore)">
      <view class="picked__main">
        <view class="picked__name">
          {{ selectedStore.name }}
          <wd-tag v-if="selectedStore.openNow === true" type="success" plain>营业中</wd-tag>
          <wd-tag v-else-if="selectedStore.openNow === false || selectedStore.businessStatus === 0" plain>
            休息中
          </wd-tag>
        </view>
        <view class="picked__addr">{{ selectedStore.address || "地址待完善" }}</view>
        <view v-if="selectedStore.distanceKm != null" class="picked__dist">
          距您 {{ formatDistance(selectedStore.distanceKm) }}
        </view>
      </view>
      <wd-button size="small" type="primary" @click.stop="goOrder(selectedStore)">
        {{ selectMode ? "选择该站" : "去下单" }}
      </wd-button>
    </view>

    <!-- 站点列表 -->
    <view class="list">
      <view class="list__header">
        <text class="list__title">附近回收站</text>
        <text class="list__count">{{ displayedStores.length ? `共 ${displayedStores.length} 家` : "" }}</text>
      </view>

      <view class="toolbar">
        <view class="toolbar__search">
          <wd-icon name="search" size="28rpx" color="#c0c4cc" />
          <input
            class="toolbar__input"
            :value="keyword"
            placeholder="搜索站名 / 地址"
            confirm-type="search"
            @input="(e: any) => (keyword = e.detail.value)"
          />
        </view>
        <view class="chips">
          <view
            class="chips__item"
            :class="{ 'chips__item--active': sortBy === 'distance' }"
            @tap="sortBy = 'distance'"
          >
            距离优先
          </view>
          <view
            class="chips__item"
            :class="{ 'chips__item--active': sortBy === 'price' }"
            @tap="sortBy = 'price'"
          >
            报价优先
          </view>
        </view>
      </view>

      <view v-if="loading" class="list__loading"><wd-loading color="#07c160" /></view>

      <template v-else>
        <view
          v-for="store in displayedStores"
          :key="store.id"
          class="store-card"
          :class="{ 'store-card--active': selectedStore?.id === store.id }"
          @tap="onRowTap(store)"
        >
          <view class="store-card__icon">
            <wd-icon name="home" size="40rpx" color="#07c160" />
          </view>
          <view class="store-card__info">
            <view class="store-card__line1">
              <text class="store-card__name">{{ store.name }}</text>
              <text v-if="store.highlightPrice != null" class="store-card__highlight">
                最高 ¥{{ store.highlightPrice }}
              </text>
              <text v-if="store.distanceKm != null" class="store-card__dist">
                {{ formatDistance(store.distanceKm) }}
              </text>
            </view>
            <view class="store-card__addr">{{ store.address || "地址待完善" }}</view>
            <view v-if="store.prices?.length" class="store-card__prices">
              <view v-for="(p, pi) in store.prices.slice(0, 3)" :key="pi" class="store-card__price-chip">
                {{ p.skuName }} ¥{{ p.price }}
              </view>
              <view v-if="store.quotedCount" class="store-card__price-more">
                {{ store.quotedCount }} 项报价
              </view>
            </view>
          </view>
          <view class="store-card__action">
            <wd-button size="small" type="primary" plain @click.stop="goOrder(store)">
              {{ selectMode ? "选择" : "去下单" }}
            </wd-button>
          </view>
        </view>

        <wd-status-tip v-if="!displayedStores.length" image="content" tip="附近暂无回收站" />
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { onLoad, onUnload } from "@dcloudio/uni-app";
import {
  getNearbyStores,
  cacheNearbyStores,
  resolveUserLocation,
  FALLBACK_LOCATION,
} from "@/api/store";
import type { StoreItem } from "@/api/store";

const stores = ref<StoreItem[]>([]);
const loading = ref(true);
const selectedStore = ref<StoreItem | null>(null);
const selectMode = ref(false);
const mapReady = ref(false);
const mapFailed = ref(false);
const keyword = ref("");
const sortBy = ref<"distance" | "price">("distance");

let center = { ...FALLBACK_LOCATION };
let map: any = null;
let leafletLib: any = null;
let markerLayer: any = null;

const displayedStores = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  let list = stores.value.filter((s) => {
    if (!kw) return true;
    return (
      (s.name || "").toLowerCase().includes(kw) ||
      (s.address || "").toLowerCase().includes(kw)
    );
  });
  if (sortBy.value === "price") {
    list = [...list].sort((a, b) => Number(b.highlightPrice || 0) - Number(a.highlightPrice || 0));
  }
  return list;
});

function formatDistance(km: number) {
  const n = Number(km);
  if (Number.isNaN(n)) return "";
  return n < 1 ? `${Math.round(n * 1000)}m` : `${n.toFixed(1)}km`;
}

function onRowTap(store: StoreItem) {
  if (selectMode.value) {
    goOrder(store);
    return;
  }
  goDetail(store);
}

function goDetail(store: StoreItem) {
  if (selectMode.value) {
    goOrder(store);
    return;
  }
  uni.navigateTo({ url: `/pages-customer/store/detail?id=${store.id}` });
}

function goOrder(store: StoreItem) {
  if (selectMode.value) {
    uni.$emit("store-selected", store);
    uni.navigateBack();
    return;
  }
  uni.navigateTo({
    url: `/pages-customer/order/create?storeId=${store.id}&storeName=${encodeURIComponent(
      store.name || ""
    )}`,
  });
}

async function loadStores() {
  loading.value = true;
  try {
    center = await resolveUserLocation();
    stores.value = (await getNearbyStores(center.longitude, center.latitude, { radiusKm: 20 })) || [];
    cacheNearbyStores(stores.value);
  } catch (e) {
    stores.value = [];
    uni.showToast({ title: "附近回收站加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
  syncMap();
}

/* ---------- H5 Leaflet 地图(非 H5 端不会调用，仅展示列表) ---------- */
async function initMap() {
  try {
    await import("leaflet/dist/leaflet.css");
    const mod: any = await import("leaflet");
    leafletLib = mod.default || mod;
    const el = document.getElementById("nearby-map");
    if (!el) throw new Error("map container missing");
    map = leafletLib
      .map(el, { zoomControl: false })
      .setView([center.latitude, center.longitude], 13);
    leafletLib
      .tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "© OpenStreetMap",
      })
      .addTo(map);
    markerLayer = leafletLib.layerGroup().addTo(map);
    mapReady.value = true;
    syncMap();
  } catch (e) {
    // 地图失败不影响列表
    mapFailed.value = true;
  }
}

function syncMap() {
  if (!mapReady.value || !map || !leafletLib) return;
  try {
    markerLayer.clearLayers();
    // 用户位置
    leafletLib
      .marker([center.latitude, center.longitude], {
        icon: leafletLib.divIcon({
          className: "",
          html: '<div class="nearby-user-dot"></div>',
          iconSize: [20, 20],
          iconAnchor: [10, 10],
        }),
      })
      .addTo(markerLayer);
    const points: Array<[number, number]> = [[center.latitude, center.longitude]];
    for (const store of stores.value) {
      const lat = Number(store.latitude);
      const lng = Number(store.longitude);
      if (!lat || !lng || Number.isNaN(lat) || Number.isNaN(lng)) continue;
      points.push([lat, lng]);
      leafletLib
        .marker([lat, lng], {
          icon: leafletLib.divIcon({
            className: "",
            html: '<div class="nearby-store-pin"><div class="nearby-store-pin__dot"></div></div>',
            iconSize: [30, 40],
            iconAnchor: [15, 40],
          }),
        })
        .on("click", () => {
          selectedStore.value = store;
        })
        .addTo(markerLayer);
    }
    if (points.length > 1) {
      map.fitBounds(points, { padding: [30, 30], maxZoom: 15 });
    } else {
      map.setView([center.latitude, center.longitude], 13);
    }
  } catch (e) {
    /* 标记渲染失败不阻塞页面 */
  }
}

onLoad((options) => {
  selectMode.value = options?.select === "1";
  loadStores();
  // #ifdef H5
  setTimeout(initMap, 0);
  // #endif
});

onUnload(() => {
  // #ifdef H5
  if (map) {
    try {
      map.remove();
    } catch (e) {
      /* ignore */
    }
    map = null;
  }
  // #endif
});
</script>

<!-- leaflet 标记为动态注入 DOM，样式不能 scoped -->
<style lang="scss">
.nearby-user-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #4d80f0;
  border: 3px solid #fff;
  box-shadow: 0 0 0 4px rgba(77, 128, 240, 0.25);
  box-sizing: border-box;
}

.nearby-store-pin {
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
}
</style>

<style lang="scss" scoped>
.nearby {
  min-height: 100vh;
  background: $page-bg;
  padding-bottom: 40rpx;

  &__map-wrap {
    position: relative;
    height: 440rpx;
    background: #e8ecef;
  }

  &__map {
    width: 100%;
    height: 100%;
  }

  &__map-loading {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 16rpx;
    color: #86909c;
    font-size: 24rpx;
    background: #f2f3f5;
    z-index: 500;
  }
}

.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
}

.picked {
  margin: 24rpx 32rpx 0;
  display: flex;
  align-items: center;
  gap: 20rpx;
  border: 2rpx solid $theme-color;

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 700;
    display: flex;
    align-items: center;
    gap: 12rpx;
  }

  &__addr {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: #4e5969;
  }

  &__dist {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $theme-color;
  }
}

.list {
  padding: 24rpx 32rpx 0;

  &__header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-bottom: 20rpx;
  }

  &__title {
    font-size: 32rpx;
    font-weight: 700;
  }

  &__count {
    font-size: 24rpx;
    color: #86909c;
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 80rpx 0;
  }
}

.toolbar {
  margin-bottom: 20rpx;

  &__search {
    display: flex;
    align-items: center;
    gap: 12rpx;
    background: #fff;
    border-radius: 16rpx;
    padding: 16rpx 20rpx;
    margin-bottom: 16rpx;
  }

  &__input {
    flex: 1;
    font-size: 26rpx;
  }
}

.chips {
  display: flex;
  gap: 16rpx;

  &__item {
    font-size: 24rpx;
    color: #4e5969;
    background: #fff;
    border-radius: 28rpx;
    padding: 10rpx 24rpx;
    border: 2rpx solid transparent;

    &--active {
      color: $theme-color;
      background: $theme-color-light;
      border-color: $theme-color;
      font-weight: 600;
    }
  }
}

.store-card {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  border: 2rpx solid transparent;

  &--active {
    border-color: $theme-color;
  }

  &__icon {
    width: 72rpx;
    height: 72rpx;
    border-radius: 20rpx;
    background: $theme-color-light;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__line1 {
    display: flex;
    align-items: baseline;
    gap: 16rpx;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 600;
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__dist {
    font-size: 24rpx;
    color: $theme-color;
    font-weight: 600;
    flex-shrink: 0;
  }

  &__addr {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: #86909c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__prices {
    margin-top: 12rpx;
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
  }

  &__highlight {
    font-size: 22rpx;
    color: #ff4d4f;
    font-weight: 600;
    flex-shrink: 0;
  }

  &__price-chip {
    font-size: 22rpx;
    color: #ff4d4f;
    background: #fff1f0;
    border-radius: 8rpx;
    padding: 4rpx 12rpx;
  }

  &__price-more {
    font-size: 22rpx;
    color: #86909c;
    padding: 4rpx 0;
  }

  &__action {
    flex-shrink: 0;
    align-self: center;
  }
}
</style>
