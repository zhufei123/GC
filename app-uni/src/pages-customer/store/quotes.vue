<template>
  <view class="quotes">
    <view class="quotes__header">
      <view class="quotes__title">{{ skuName || "门店比价" }}</view>
      <view class="quotes__sub">
        {{ isGuideFallback ? "以下为平台指导价 · 门店实报价开通中" : "各回收站实时报价 · 卖前先比价" }}
      </view>
    </view>

    <!-- 排序 -->
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
        价格优先
      </view>
    </view>

    <view v-if="loading" class="quotes__loading"><wd-loading color="#07c160" /></view>

    <template v-else>
      <view
        v-for="(q, i) in sortedQuotes"
        :key="q.storeId"
        class="quote-card"
        @tap="goStore(q)"
      >
        <view class="quote-card__rank" :class="{ 'quote-card__rank--top': i === 0 }">
          {{ i + 1 }}
        </view>
        <view class="quote-card__info">
          <view class="quote-card__name">{{ q.storeName || q.name }}</view>
          <view class="quote-card__meta">
            <text v-if="q.distanceKm != null">{{ formatDistance(q.distanceKm) }}</text>
            <text v-if="q.address" class="quote-card__addr">{{ q.address }}</text>
          </view>
        </view>
        <view class="quote-card__price">
          <template v-if="q.price != null && q.price !== ''">
            <text class="quote-card__amount">¥{{ q.price }}</text>
            <text class="quote-card__unit">/{{ q.unit || "kg" }}</text>
            <view v-if="q.guide" class="quote-card__guide">指导价</view>
          </template>
          <wd-tag v-else plain>暂无报价</wd-tag>
        </view>
      </view>

      <wd-status-tip v-if="!quotes.length" image="content" tip="附近暂无门店报价" />
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getSkuQuotes, getNearbyStores, cacheNearbyStores, resolveUserLocation } from "@/api/store";
import type { SkuQuoteItem } from "@/api/store";
import { getSkuList } from "@/api/goods";

const skuId = ref("");
const skuName = ref("");
const quotes = ref<SkuQuoteItem[]>([]);
const loading = ref(true);
const sortBy = ref<"distance" | "price">("distance");
const isGuideFallback = ref(false);

const sortedQuotes = computed(() => {
  const list = [...quotes.value];
  if (sortBy.value === "price") {
    // 卖方视角：价格高者优先，无报价垫底
    list.sort((a, b) => toPrice(b) - toPrice(a));
  } else {
    list.sort((a, b) => toDistance(a) - toDistance(b));
  }
  return list;
});

function toPrice(q: SkuQuoteItem) {
  const n = parseFloat(String(q.price ?? ""));
  return Number.isNaN(n) ? -1 : n;
}

function toDistance(q: SkuQuoteItem) {
  const n = Number(q.distanceKm);
  return q.distanceKm == null || Number.isNaN(n) ? Number.MAX_SAFE_INTEGER : n;
}

function formatDistance(km: number) {
  const n = Number(km);
  if (Number.isNaN(n)) return "";
  return n < 1 ? `${Math.round(n * 1000)}m` : `${n.toFixed(1)}km`;
}

function goStore(q: SkuQuoteItem) {
  uni.navigateTo({ url: `/pages-customer/store/detail?id=${q.storeId}` });
}

async function load() {
  loading.value = true;
  try {
    const { longitude, latitude } = await resolveUserLocation();
    try {
      const list = await getSkuQuotes(skuId.value, longitude, latitude);
      if (Array.isArray(list) && list.length) {
        quotes.value = list;
        isGuideFallback.value = list.every((q) => q.guide === true);
        return;
      }
      throw new Error("empty quotes");
    } catch (e) {
      // 报价接口未就绪：附近门店 + SKU 指导价兜底
      await loadFallback(longitude, latitude);
    }
  } finally {
    loading.value = false;
  }
}

async function loadFallback(longitude: number, latitude: number) {
  try {
    const [stores, skus] = await Promise.all([
      getNearbyStores(longitude, latitude).catch(() => []),
      getSkuList().catch(() => []),
    ]);
    cacheNearbyStores(stores || []);
    const sku = (skus || []).find((s) => String(s.id) === String(skuId.value));
    quotes.value = (stores || []).map((store) => {
      const storePrice = store.prices?.find((p) => String(p.skuId) === String(skuId.value));
      return {
        storeId: store.id,
        storeName: store.name,
        address: store.address,
        distanceKm: store.distanceKm,
        price: storePrice?.price ?? sku?.price ?? null,
        unit: storePrice?.unit || sku?.unit,
        guide: !storePrice,
      };
    });
    isGuideFallback.value = quotes.value.length > 0 && quotes.value.every((q) => q.guide);
    if (!quotes.value.length) {
      uni.showToast({ title: "暂无门店报价数据", icon: "none" });
    }
  } catch (e) {
    quotes.value = [];
    uni.showToast({ title: "报价加载失败", icon: "none" });
  }
}

onLoad((options) => {
  skuId.value = options?.skuId ? String(options.skuId) : "";
  skuName.value = options?.skuName ? decodeURIComponent(String(options.skuName)) : "";
  if (skuName.value) {
    uni.setNavigationBarTitle({ title: `${skuName.value} · 比价` });
  }
  if (!skuId.value) {
    uni.showToast({ title: "缺少品类参数", icon: "none" });
    loading.value = false;
    return;
  }
  load();
});
</script>

<style lang="scss" scoped>
.quotes {
  min-height: 100vh;
  background: $page-bg;
  padding-bottom: 40rpx;

  &__header {
    background: $theme-gradient;
    padding: 32rpx 32rpx 28rpx;
  }

  &__title {
    color: #fff;
    font-size: 36rpx;
    font-weight: 700;
  }

  &__sub {
    margin-top: 8rpx;
    color: rgba(255, 255, 255, 0.85);
    font-size: 24rpx;
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 80rpx 0;
  }
}

.chips {
  display: flex;
  gap: 20rpx;
  padding: 24rpx 32rpx 8rpx;

  &__item {
    padding: 12rpx 32rpx;
    border-radius: 32rpx;
    background: #fff;
    font-size: 26rpx;
    color: #4e5969;
    border: 2rpx solid transparent;

    &--active {
      background: $theme-color-light;
      color: $theme-color;
      border-color: $theme-color;
      font-weight: 600;
    }
  }
}

.quote-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin: 20rpx 32rpx 0;

  &__rank {
    width: 48rpx;
    height: 48rpx;
    border-radius: 50%;
    background: #f2f3f5;
    color: #86909c;
    font-size: 26rpx;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &--top {
      background: $theme-color;
      color: #fff;
    }
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: #86909c;
    display: flex;
    gap: 16rpx;
    align-items: baseline;
  }

  &__addr {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__price {
    flex-shrink: 0;
    text-align: right;
  }

  &__amount {
    color: #ff4d4f;
    font-size: 34rpx;
    font-weight: 700;
  }

  &__unit {
    color: #86909c;
    font-size: 22rpx;
  }

  &__guide {
    margin-top: 4rpx;
    font-size: 20rpx;
    color: #ff8f1f;
  }
}
</style>
