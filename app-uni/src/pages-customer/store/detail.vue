<template>
  <view class="detail">
    <view v-if="loading" class="detail__loading"><wd-loading color="#07c160" /></view>

    <template v-else-if="store">
      <!-- 门店信息 -->
      <view class="card head">
        <view class="head__top">
          <view class="head__icon">
            <wd-icon name="home" size="48rpx" color="#07c160" />
          </view>
          <view class="head__main">
            <view class="head__name">
              {{ store.name }}
              <wd-tag v-if="store.businessStatus === 0" plain>休息中</wd-tag>
              <wd-tag v-else-if="store.businessStatus === 1" type="success" plain>营业中</wd-tag>
            </view>
            <view v-if="store.distanceKm != null" class="head__dist">
              距您 {{ formatDistance(store.distanceKm) }}
            </view>
          </view>
        </view>

        <view class="head__row">
          <wd-icon name="time" size="32rpx" color="#86909c" />
          <text>{{ store.businessHours || "营业时间待完善" }}</text>
        </view>
        <view class="head__row" @tap="callStore">
          <wd-icon name="phone" size="32rpx" color="#86909c" />
          <text :class="{ head__link: !!store.phone }">{{ store.phone || "电话待完善" }}</text>
        </view>
        <view class="head__row">
          <wd-icon name="location" size="32rpx" color="#86909c" />
          <text class="head__addr">{{ store.address || "地址待完善" }}</text>
          <wd-button v-if="canNavigate" size="small" plain @click.stop="openNav">导航</wd-button>
        </view>
      </view>

      <!-- 回收价目 -->
      <view class="card">
        <view class="card__title">
          回收价目
          <wd-tag v-if="priceIsGuide" plain custom-class="card__title-tag">指导价</wd-tag>
          <wd-tag v-else type="success" plain custom-class="card__title-tag">门店报价</wd-tag>
        </view>
        <view v-if="priceLoading" class="detail__loading--inline"><wd-loading color="#07c160" /></view>
        <template v-else>
          <view v-for="p in prices" :key="p.skuId" class="price-row">
            <text class="price-row__name">{{ p.skuName || p.name }}</text>
            <view class="price-row__right">
              <template v-if="p.price != null && p.price !== ''">
                <text class="price-row__amount">¥{{ p.price }}</text>
                <text class="price-row__unit">/{{ p.unit || "kg" }}</text>
              </template>
              <wd-tag v-else plain>暂无报价</wd-tag>
            </view>
          </view>
          <wd-status-tip v-if="!prices.length" image="content" tip="暂无价目信息" />
        </template>
      </view>
    </template>

    <wd-status-tip v-else image="search" tip="回收站不存在或已下线" />

    <!-- 底部下单入口 -->
    <view v-if="store" class="footer">
      <wd-button plain custom-class="footer__btn" @click="goOrder('DROPOFF')">
        自行送货
      </wd-button>
      <wd-button type="primary" custom-class="footer__btn" @click="goOrder('PICKUP')">
        上门回收
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getStoreDetail, getStorePrices, getCachedStore } from "@/api/store";
import type { StoreItem, StorePriceItem } from "@/api/store";
import { getSkuList } from "@/api/goods";

const store = ref<StoreItem | null>(null);
const prices = ref<StorePriceItem[]>([]);
const priceIsGuide = ref(false);
const loading = ref(true);
const priceLoading = ref(true);

const canNavigate = computed(() => {
  const lat = Number(store.value?.latitude);
  const lng = Number(store.value?.longitude);
  return !!lat && !!lng && !Number.isNaN(lat) && !Number.isNaN(lng);
});

function formatDistance(km: number) {
  const n = Number(km);
  if (Number.isNaN(n)) return "";
  return n < 1 ? `${Math.round(n * 1000)}m` : `${n.toFixed(1)}km`;
}

async function loadStore(id: string) {
  loading.value = true;
  try {
    store.value = await getStoreDetail(id);
  } catch (e) {
    // 详情接口未就绪：使用附近列表缓存兜底
    store.value = getCachedStore(id);
    if (!store.value) {
      uni.showToast({ title: "门店信息加载失败", icon: "none" });
    }
  } finally {
    loading.value = false;
  }
}

async function loadPrices(id: string) {
  priceLoading.value = true;
  try {
    const list = await getStorePrices(id);
    if (Array.isArray(list) && list.length) {
      prices.value = list;
      priceIsGuide.value = list.every((p) => p.guide === true);
      return;
    }
    throw new Error("empty store prices");
  } catch (e) {
    // 门店价目未就绪：回退平台 SKU 指导价
    try {
      const skus = (await getSkuList()) || [];
      prices.value = skus.map((s) => ({
        skuId: s.id,
        skuName: s.name,
        price: s.price,
        unit: s.unit,
        guide: true,
      }));
      priceIsGuide.value = true;
    } catch (e2) {
      prices.value = [];
    }
  } finally {
    priceLoading.value = false;
  }
}

function callStore() {
  const phone = store.value?.phone;
  if (!phone) return;
  uni.makePhoneCall({ phoneNumber: phone });
}

function openNav() {
  if (!canNavigate.value || !store.value) return;
  const lat = Number(store.value.latitude);
  const lng = Number(store.value.longitude);
  // #ifdef H5
  window.open(`https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=17/${lat}/${lng}`);
  // #endif
  // #ifndef H5
  uni.openLocation({
    latitude: lat,
    longitude: lng,
    name: store.value.name,
    address: store.value.address || "",
  });
  // #endif
}

function goOrder(type: "PICKUP" | "DROPOFF") {
  if (!store.value) return;
  uni.navigateTo({
    url: `/pages-customer/order/create?storeId=${store.value.id}&storeName=${encodeURIComponent(
      store.value.name || ""
    )}&type=${type}`,
  });
}

onLoad((options) => {
  const id = options?.id ? String(options.id) : "";
  if (!id) {
    uni.showToast({ title: "缺少门店参数", icon: "none" });
    loading.value = false;
    priceLoading.value = false;
    return;
  }
  loadStore(id);
  loadPrices(id);
});
</script>

<style lang="scss" scoped>
.detail {
  min-height: 100vh;
  background: $page-bg;
  padding: 24rpx 32rpx 180rpx;
  box-sizing: border-box;

  &__loading {
    display: flex;
    justify-content: center;
    padding: 120rpx 0;

    &--inline {
      display: flex;
      justify-content: center;
      padding: 48rpx 0;
    }
  }
}

.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;

  &__title {
    font-size: 30rpx;
    font-weight: 700;
    margin-bottom: 24rpx;
    display: flex;
    align-items: center;
    gap: 16rpx;
  }
}

.head {
  &__top {
    display: flex;
    align-items: center;
    gap: 20rpx;
    padding-bottom: 24rpx;
    border-bottom: 1rpx solid #f2f3f5;
    margin-bottom: 20rpx;
  }

  &__icon {
    width: 88rpx;
    height: 88rpx;
    border-radius: 24rpx;
    background: $theme-color-light;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 34rpx;
    font-weight: 700;
    display: flex;
    align-items: center;
    gap: 12rpx;
  }

  &__dist {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $theme-color;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 16rpx;
    padding: 14rpx 0;
    font-size: 26rpx;
    color: #4e5969;
  }

  &__link {
    color: $theme-color;
  }

  &__addr {
    flex: 1;
    min-width: 0;
  }
}

.price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f7f8fa;

  &:last-child {
    border-bottom: none;
  }

  &__name {
    font-size: 28rpx;
  }

  &__amount {
    color: #ff4d4f;
    font-size: 30rpx;
    font-weight: 700;
  }

  &__unit {
    color: #86909c;
    font-size: 22rpx;
  }
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  display: flex;
  gap: 24rpx;
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -6rpx 20rpx rgba(31, 35, 41, 0.06);

  :deep(.footer__btn) {
    flex: 1;
    border-radius: 44rpx !important;
  }
}
</style>
