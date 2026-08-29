<template>
  <scroll-view scroll-y class="home" :style="{ height: 'calc(100vh - 100rpx)' }">
    <view class="home__header">
      <view class="home__topbar">
        <view class="home__city" @tap="openCityPicker">
          <wd-icon name="location" size="32rpx" color="#ffffff" />
          <text>{{ cityLabel }}</text>
          <wd-icon name="caret-down-small" size="28rpx" color="#ffffff" />
        </view>
        <view class="home__brand">绿色回收</view>
      </view>
      <view class="home__search" @tap="goSearch">
        <wd-icon name="search" size="32rpx" color="#9ca3af" />
        <text>搜索废纸 / 金属 / 塑料回收价</text>
      </view>
    </view>

    <view class="home__body">
      <!-- Banner -->
      <view class="banner">
        <wd-swiper
          v-if="banners.length"
          :list="banners"
          autoplay
          height="280rpx"
          custom-class="banner__swiper"
        />
        <view v-else class="banner__fallback">
          <view class="banner__fallback-title">变废为宝 · 绿色生活</view>
          <view class="banner__fallback-sub">上门回收，足不出户环保变现</view>
          <wd-icon name="refresh" size="120rpx" color="rgba(255,255,255,0.35)" custom-class="banner__fallback-icon" />
        </view>
      </view>

      <!-- 公告 -->
      <wd-notice-bar
        v-if="noticeText"
        :text="noticeText"
        prefix="sound"
        color="#07c160"
        background-color="#e8f9ef"
        custom-class="home__notice"
      />

      <!-- 分类宫格 -->
      <view class="section">
        <view class="section__title">回收分类</view>
        <view class="cat-grid">
          <view
            v-for="(cat, i) in categories"
            :key="cat.id"
            class="cat-grid__item"
            @tap="$emit('switch-tab', 1, cat.id)"
          >
            <view class="cat-grid__icon" :style="{ background: CAT_COLORS[i % CAT_COLORS.length].bg }">
              <wd-icon
                :name="CAT_COLORS[i % CAT_COLORS.length].icon"
                size="44rpx"
                :color="CAT_COLORS[i % CAT_COLORS.length].color"
              />
            </view>
            <text class="cat-grid__name">{{ cat.name }}</text>
          </view>
          <view v-if="!categories.length" class="cat-grid__empty">分类加载中…</view>
        </view>
      </view>

      <!-- 上门回收入口 -->
      <view class="pickup-card" @tap="goCreate">
        <view class="pickup-card__left">
          <view class="pickup-card__title">预约上门回收</view>
          <view class="pickup-card__desc">回收小哥免费上门 · 当面称重结算</view>
          <view class="pickup-card__btn">
            立即预约
            <wd-icon name="arrow-right" size="26rpx" color="#ffffff" />
          </view>
        </view>
        <view class="pickup-card__right">
          <wd-icon name="service" size="96rpx" color="rgba(255,255,255,0.9)" />
        </view>
      </view>

      <!-- 附近回收站入口 -->
      <view class="store-entry" @tap="goNearby">
        <view class="store-entry__icon">
          <wd-icon name="location" size="48rpx" color="#4d80f0" />
        </view>
        <view class="store-entry__main">
          <view class="store-entry__title">附近回收站</view>
          <view class="store-entry__desc">地图找站 · 到店卖 · 比价更划算</view>
        </view>
        <view class="store-entry__go">
          去看看
          <wd-icon name="arrow-right" size="26rpx" color="#4d80f0" />
        </view>
      </view>

      <!-- 次要入口 -->
      <view class="quick-row">
        <view class="quick-row__item" @tap="$emit('switch-tab', 1)">
          <wd-icon name="money-circle" size="44rpx" color="#07c160" />
          <view>
            <view class="quick-row__title">今日回收价</view>
            <view class="quick-row__desc">指导价 · 门店比价</view>
          </view>
        </view>
        <view class="quick-row__item" @tap="$emit('switch-tab', 2)">
          <wd-icon name="list" size="44rpx" color="#ff8f1f" />
          <view>
            <view class="quick-row__title">我的订单</view>
            <view class="quick-row__desc">查看回收进度</view>
          </view>
        </view>
      </view>

      <view class="home__footer">— 绿色回收，让地球更轻盈 —</view>
    </view>

    <!-- 城市选择：省 → 市 二级联动 -->
    <wd-action-sheet v-model="cityPickerVisible" title="选择城市">
      <view v-if="cityPickerVisible" class="city-picker">
        <view
          class="city-picker__item city-picker__item--locate"
          :class="{ 'city-picker__item--active': !locationStore.city }"
          @tap="useCurrentLocation"
        >
          <view class="city-picker__locate">
            <wd-icon name="aim" size="32rpx" color="#07c160" />
            <text>使用当前位置</text>
          </view>
          <wd-icon v-if="!locationStore.city" name="check" size="32rpx" color="#07c160" />
        </view>

        <view class="city-picker__hots">
          <view
            v-for="h in hotCities"
            :key="h.name"
            class="city-picker__hot"
            :class="{ 'city-picker__hot--on': isCityActive(h.name) }"
            @tap="pickCity(h, h.province)"
          >
            {{ h.name }}
          </view>
        </view>

        <view class="city-picker__search">
          <wd-icon name="search" size="28rpx" color="#9ca3af" />
          <input
            v-model="cityKeyword"
            class="city-picker__search-input"
            placeholder="搜索城市，如 杭州 / 成都"
            confirm-type="search"
          />
        </view>

        <view v-if="cityKeyword.trim()" class="city-picker__search-list">
          <view
            v-for="hit in searchHits"
            :key="hit.province + hit.name"
            class="city-picker__item"
            :class="{ 'city-picker__item--active': isCityActive(hit.name) }"
            @tap="pickCity(hit, hit.province)"
          >
            <text>{{ hit.province }} · {{ hit.name }}</text>
            <wd-icon v-if="isCityActive(hit.name)" name="check" size="32rpx" color="#07c160" />
          </view>
          <view v-if="!searchHits.length" class="city-picker__empty">未找到该城市</view>
        </view>

        <view v-else class="city-picker__cols">
          <scroll-view scroll-y class="city-picker__prov">
            <view
              v-for="p in provinces"
              :key="p.name"
              class="city-picker__prov-item"
              :class="{ 'city-picker__prov-item--on': p.name === activeProvince }"
              @tap="activeProvince = p.name"
            >
              {{ p.name }}
            </view>
          </scroll-view>
          <scroll-view scroll-y class="city-picker__city">
            <view
              v-for="c in activeCities"
              :key="c.name"
              class="city-picker__item"
              :class="{ 'city-picker__item--active': isCityActive(c.name) }"
              @tap="pickCity(c, activeProvince)"
            >
              <text>{{ c.name }}</text>
              <wd-icon v-if="isCityActive(c.name)" name="check" size="32rpx" color="#07c160" />
            </view>
            <view v-if="cityLoading" class="city-picker__empty">城市加载中…</view>
            <view v-else-if="!activeCities.length" class="city-picker__empty">暂无城市</view>
          </scroll-view>
        </view>
      </view>
    </wd-action-sheet>
  </scroll-view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { getHome } from "@/api/home";
import { getCategoryTree } from "@/api/goods";
import { getStoreRegions, type RegionCity, type RegionProvince } from "@/api/store";
import { useLocationStore } from "@/store/location";

defineEmits<{ (e: "switch-tab", index: number, categoryId?: string): void }>();

const CAT_COLORS = [
  { icon: "read", color: "#07c160", bg: "#e8f9ef" },
  { icon: "tools", color: "#4d80f0", bg: "#e8f0fe" },
  { icon: "goods", color: "#ff8f1f", bg: "#fff3e5" },
  { icon: "computer", color: "#8f5cf0", bg: "#f1eafe" },
  { icon: "bags", color: "#f0508a", bg: "#fdeaf1" },
  { icon: "gift", color: "#00b4c5", bg: "#e0f7fa" },
  { icon: "cart", color: "#c99f3f", bg: "#faf3e0" },
  { icon: "more", color: "#86909c", bg: "#f2f3f5" },
];

const banners = ref<string[]>([]);
const categories = ref<Array<{ id: string; name: string }>>([]);
const notices = ref<Array<{ title: string }>>([]);
let loaded = false;

const noticeText = computed(() => notices.value.map((n) => n.title).join("    "));

/* ---------- 城市选择 ---------- */

const locationStore = useLocationStore();
const cityPickerVisible = ref(false);
const cityLoading = ref(false);
const provinces = ref<RegionProvince[]>([]);
const activeProvince = ref("广东省");
const cityKeyword = ref("");

const HOT_NAMES = ["北京市", "上海市", "广州市", "深圳市", "杭州市", "成都市"];

const cityLabel = computed(() => locationStore.city || "请选择城市");

function normCity(s?: string) {
  return (s || "").replace(/市$/, "");
}

function isCityActive(city: string) {
  const stored = locationStore.city;
  if (!stored) return false;
  return normCity(stored) === normCity(city);
}

const activeCities = computed<RegionCity[]>(() => {
  const p = provinces.value.find((x) => x.name === activeProvince.value);
  return p?.cities || [];
});

const hotCities = computed(() => {
  const hits: Array<RegionCity & { province: string }> = [];
  for (const name of HOT_NAMES) {
    for (const p of provinces.value) {
      const c = (p.cities || []).find((x) => x.name === name);
      if (c) {
        hits.push({ ...c, province: p.name });
        break;
      }
    }
  }
  return hits;
});

const searchHits = computed(() => {
  const kw = cityKeyword.value.trim();
  if (!kw) return [];
  const hits: Array<RegionCity & { province: string }> = [];
  for (const p of provinces.value) {
    for (const c of p.cities || []) {
      if (c.name.includes(kw) || p.name.includes(kw)) {
        hits.push({ ...c, province: p.name });
      }
    }
  }
  return hits.slice(0, 40);
});

async function openCityPicker() {
  cityPickerVisible.value = true;
  cityKeyword.value = "";
  if (!provinces.value.length) {
    cityLoading.value = true;
    try {
      provinces.value = (await getStoreRegions()) || [];
    } catch (e) {
      /* 空态 */
    } finally {
      cityLoading.value = false;
    }
  }
  if (locationStore.province) {
    activeProvince.value = locationStore.province;
  } else if (locationStore.city) {
    const found = provinces.value.find((p) =>
      (p.cities || []).some((c) => isCityActive(c.name))
    );
    if (found) activeProvince.value = found.name;
  }
}

function pickCity(c: RegionCity, province?: string) {
  locationStore.setCity(c.name, Number(c.longitude) || 0, Number(c.latitude) || 0, province);
  cityPickerVisible.value = false;
}

/** 清除已选城市，回退到 GPS/地址定位 */
function useCurrentLocation() {
  locationStore.clear();
  cityPickerVisible.value = false;
  uni.showToast({ title: "已切换为当前位置", icon: "none" });
}

async function refresh(force = false) {
  if (loaded && !force) return;
  loaded = true;
  try {
    const home = await getHome();
    banners.value = (home?.banners || [])
      .map((b) => b.image || b.imageUrl || "")
      .filter((s) => !!s);
    notices.value = home?.notices || [];
    if (home?.hotCategories?.length) {
      categories.value = home.hotCategories;
      return;
    }
  } catch (e) {
    /* 首页接口失败时降级到分类树 */
  }
  try {
    const tree = await getCategoryTree();
    categories.value = (tree || []).map((n) => ({ id: n.id, name: n.name }));
  } catch (e) {
    /* 保持空态 */
  } finally {
    // 分类仍为空则允许下次进入重试
    if (!categories.value.length) loaded = false;
  }
}

function goSearch() {
  uni.navigateTo({ url: "/pages-customer/search/index" });
}

function goCreate() {
  uni.navigateTo({ url: "/pages-customer/order/create" });
}

function goNearby() {
  uni.navigateTo({ url: "/pages-customer/store/nearby" });
}

refresh();
defineExpose({ refresh });
</script>

<style lang="scss" scoped>
.home {
  &__header {
    background: $theme-gradient;
    padding: 24rpx 32rpx 88rpx;
  }

  &__topbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12rpx 0 24rpx;
  }

  &__city {
    display: flex;
    align-items: center;
    gap: 6rpx;
    color: #fff;
    font-size: 30rpx;
    font-weight: 600;
  }

  &__brand {
    color: rgba(255, 255, 255, 0.9);
    font-size: 28rpx;
    letter-spacing: 4rpx;
  }

  &__search {
    display: flex;
    align-items: center;
    gap: 14rpx;
    background: #fff;
    height: 76rpx;
    border-radius: 38rpx;
    padding: 0 28rpx;
    color: #c0c4cc;
    font-size: 26rpx;
  }

  &__body {
    margin-top: -56rpx;
    padding: 0 32rpx 40rpx;
  }

  &__footer {
    text-align: center;
    color: #c0c4cc;
    font-size: 22rpx;
    padding: 40rpx 0 24rpx;
  }
}

.banner {
  border-radius: 24rpx;
  overflow: hidden;

  &__fallback {
    position: relative;
    height: 280rpx;
    border-radius: 24rpx;
    background: linear-gradient(120deg, #0aa653 0%, #10d576 60%, #6ee7a8 100%);
    padding: 56rpx 40rpx;
    overflow: hidden;

    &-title {
      color: #fff;
      font-size: 40rpx;
      font-weight: 700;
    }

    &-sub {
      margin-top: 16rpx;
      color: rgba(255, 255, 255, 0.85);
      font-size: 26rpx;
    }
  }

  :deep(.banner__fallback-icon) {
    position: absolute;
    right: 32rpx;
    bottom: 24rpx;
  }
}

:deep(.home__notice) {
  margin-top: 24rpx;
  border-radius: 16rpx;
}

.section {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;

  &__title {
    font-size: 30rpx;
    font-weight: 700;
    margin-bottom: 24rpx;
  }
}

.cat-grid {
  display: flex;
  flex-wrap: wrap;

  &__item {
    width: 25%;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12rpx;
    padding: 16rpx 0;
  }

  &__icon {
    width: 88rpx;
    height: 88rpx;
    border-radius: 28rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__name {
    font-size: 24rpx;
    color: #4e5969;
  }

  &__empty {
    width: 100%;
    text-align: center;
    color: #c0c4cc;
    font-size: 24rpx;
    padding: 24rpx 0;
  }
}

.pickup-card {
  margin-top: 24rpx;
  border-radius: 24rpx;
  background: linear-gradient(120deg, #07c160, #0fb85f 55%, #3ccf82);
  padding: 36rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 12rpx 32rpx rgba(7, 193, 96, 0.25);

  &__title {
    color: #fff;
    font-size: 36rpx;
    font-weight: 700;
  }

  &__desc {
    margin-top: 10rpx;
    color: rgba(255, 255, 255, 0.85);
    font-size: 24rpx;
  }

  &__btn {
    margin-top: 24rpx;
    display: inline-flex;
    align-items: center;
    gap: 6rpx;
    background: rgba(255, 255, 255, 0.22);
    color: #fff;
    font-size: 26rpx;
    font-weight: 600;
    padding: 12rpx 28rpx;
    border-radius: 32rpx;
  }
}

.store-entry {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;

  &__icon {
    width: 88rpx;
    height: 88rpx;
    border-radius: 24rpx;
    background: #e8f0fe;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: 30rpx;
    font-weight: 700;
  }

  &__desc {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: #86909c;
  }

  &__go {
    display: flex;
    align-items: center;
    gap: 4rpx;
    color: #4d80f0;
    font-size: 26rpx;
    font-weight: 600;
    flex-shrink: 0;
  }
}

.city-picker {
  max-height: 70vh;
  background: #fff;
  padding: 0 0 16rpx;

  &__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24rpx 32rpx;
    border-bottom: 1rpx solid #f2f3f5;
    font-size: 28rpx;

    &--active {
      color: $theme-color;
      font-weight: 600;
    }
  }

  &__locate {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }

  &__hots {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    padding: 16rpx 32rpx 8rpx;
  }

  &__hot {
    padding: 10rpx 22rpx;
    border-radius: 28rpx;
    background: #f5f6f8;
    font-size: 24rpx;
    color: #4e5969;

    &--on {
      background: #e8f9ef;
      color: $theme-color;
      font-weight: 600;
    }
  }

  &__search {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin: 12rpx 32rpx 16rpx;
    background: #f5f6f8;
    border-radius: 32rpx;
    padding: 0 24rpx;
    height: 64rpx;
  }

  &__search-input {
    flex: 1;
    font-size: 26rpx;
    height: 64rpx;
  }

  &__search-list {
    max-height: 46vh;
    overflow-y: auto;
  }

  &__cols {
    display: flex;
    height: 52vh;
    border-top: 1rpx solid #f2f3f5;
  }

  &__prov {
    width: 240rpx;
    background: #f7f8fa;
    height: 100%;
  }

  &__prov-item {
    padding: 24rpx 20rpx;
    font-size: 26rpx;
    color: #4e5969;

    &--on {
      background: #fff;
      color: $theme-color;
      font-weight: 600;
    }
  }

  &__city {
    flex: 1;
    height: 100%;
  }

  &__empty {
    text-align: center;
    color: #c0c4cc;
    padding: 48rpx 0;
    font-size: 26rpx;
  }
}

.quick-row {
  margin-top: 24rpx;
  display: flex;
  gap: 24rpx;

  &__item {
    flex: 1;
    background: #fff;
    border-radius: 24rpx;
    padding: 28rpx;
    display: flex;
    align-items: center;
    gap: 20rpx;
  }

  &__title {
    font-size: 28rpx;
    font-weight: 600;
  }

  &__desc {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: #86909c;
  }
}
</style>
