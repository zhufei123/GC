<template>
  <view class="detail">
    <view v-if="loading" class="detail__loading"><wd-loading color="#07c160" /></view>

    <template v-else-if="store">
      <!-- 门店照片 -->
      <view v-if="photos.length" class="gallery">
        <swiper
          class="gallery__swiper"
          :indicator-dots="photos.length > 1"
          indicator-active-color="#07c160"
          circular
          :autoplay="photos.length > 1"
          :interval="4000"
        >
          <swiper-item v-for="(photo, pi) in photos" :key="pi" @tap="previewPhoto(pi)">
            <image class="gallery__img" :src="photo" mode="aspectFill" />
          </swiper-item>
        </swiper>
      </view>

      <!-- 门店信息 -->
      <view class="card head">
        <view class="head__top">
          <view class="head__icon">
            <wd-icon name="home" size="48rpx" color="#07c160" />
          </view>
          <view class="head__main">
            <view class="head__name">
              {{ store.name }}
              <wd-tag v-if="store.openNow === true" type="success" plain>营业中</wd-tag>
              <wd-tag
                v-else-if="store.openNow === false || store.businessStatus === 0"
                plain
              >
                休息中
              </wd-tag>
            </view>
            <view class="head__meta">
              <view v-if="store.avgRating != null" class="head__rating">
                <wd-icon name="star-filled" size="26rpx" color="#ff8f1f" />
                <text class="head__rating-score">{{ store.avgRating }}</text>
                <text class="head__rating-count">{{ store.reviewCount || 0 }}条评价</text>
              </view>
              <view v-if="store.distanceKm != null" class="head__dist">
                距您 {{ formatDistance(store.distanceKm) }}
              </view>
            </view>
          </view>
          <view class="head__fav" :class="{ 'head__fav--on': favorited }" @tap.stop="toggleFavorite">
            <wd-icon
              :name="favorited ? 'heart-filled' : 'heart'"
              size="40rpx"
              :color="favorited ? '#ff4d4f' : '#c0c4cc'"
            />
            <text class="head__fav-text">{{ favorited ? "已收藏" : "收藏" }}</text>
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
                <view v-if="!p.guide && p.guidePrice != null" class="price-row__guide">
                  指导价 ¥{{ p.guidePrice }}
                </view>
              </template>
              <wd-tag v-else plain>暂无报价</wd-tag>
            </view>
          </view>
          <wd-status-tip v-if="!prices.length" image="content" tip="暂无价目信息" />
        </template>
      </view>

      <!-- 用户评价 -->
      <view class="card">
        <view class="card__title">用户评价</view>
        <template v-if="reviews.length">
          <view v-for="(r, ri) in reviews" :key="r.id || ri" class="review-row">
            <view class="review-row__head">
              <wd-rate :model-value="r.rating" readonly size="14px" active-color="#ff8f1f" />
              <text class="review-row__nick">{{ maskNickname(r.nickname) }}</text>
              <text class="review-row__time">{{ r.createTime || "" }}</text>
            </view>
            <view v-if="r.comment" class="review-row__comment">{{ r.comment }}</view>
          </view>
        </template>
        <view v-else class="review-empty">暂无评价</view>
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
import {
  getStoreDetail,
  getStorePrices,
  getStoreReviews,
  getCachedStore,
  resolveUserLocation,
  isStationFavorite,
  favoriteStation,
  unfavoriteStation,
} from "@/api/store";
import type { StoreItem, StorePriceItem, StoreReviewItem } from "@/api/store";
import { getSkuList } from "@/api/goods";
import { openNavigation } from "@/utils/map-nav";
import { useUserStore } from "@/store/user";

const userStore = useUserStore();
const store = ref<StoreItem | null>(null);
const prices = ref<StorePriceItem[]>([]);
const reviews = ref<StoreReviewItem[]>([]);
const priceIsGuide = ref(false);
const loading = ref(true);
const priceLoading = ref(true);
const favorited = ref(false);
let favoritePending = false;
let storeId = "";

const canNavigate = computed(() => {
  const lat = Number(store.value?.latitude);
  const lng = Number(store.value?.longitude);
  return !!lat && !!lng && !Number.isNaN(lat) && !Number.isNaN(lng);
});

const photos = computed(() =>
  (store.value?.photos || []).filter((p) => typeof p === "string" && p.length > 0)
);

function previewPhoto(index: number) {
  uni.previewImage({ urls: photos.value, current: index });
}

function formatDistance(km: number) {
  const n = Number(km);
  if (Number.isNaN(n)) return "";
  return n < 1 ? `${Math.round(n * 1000)}m` : `${n.toFixed(1)}km`;
}

async function loadStore(id: string) {
  loading.value = true;
  try {
    // 带用户坐标查询，后端返回 distanceKm
    const { longitude, latitude } = await resolveUserLocation();
    store.value = await getStoreDetail(id, longitude, latitude);
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
    // status=0 停报的条目不展示
    const active = (list || []).filter((p) => p.status !== 0);
    if (active.length) {
      prices.value = active;
      priceIsGuide.value = active.every((p) => p.guide === true);
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

/** 兜底脱敏：后端已脱敏时原样展示 */
function maskNickname(nick?: string) {
  const n = (nick || "").trim();
  if (!n) return "匿名用户";
  if (n.includes("*")) return n;
  if (n.length <= 1) return `${n}**`;
  return `${n[0]}**${n[n.length - 1]}`;
}

async function loadReviews(id: string) {
  try {
    reviews.value = (await getStoreReviews(id)) || [];
  } catch (e) {
    reviews.value = [];
  }
}

/** 未登录不查收藏态，避免触发 40100 被踢去登录页 */
async function loadFavorite(id: string) {
  if (!userStore.isLogin) return;
  try {
    favorited.value = (await isStationFavorite(id)) === true;
  } catch (e) {
    /* 收藏态查询失败保持默认 */
  }
}

async function toggleFavorite() {
  if (!storeId || favoritePending) return;
  if (!userStore.isLogin) {
    uni.showToast({ title: "请先登录", icon: "none" });
    return;
  }
  favoritePending = true;
  const next = !favorited.value;
  favorited.value = next;
  try {
    if (next) {
      await favoriteStation(storeId);
      uni.showToast({ title: "已收藏", icon: "none" });
    } else {
      await unfavoriteStation(storeId);
      uni.showToast({ title: "已取消收藏", icon: "none" });
    }
  } catch (e) {
    favorited.value = !next;
  } finally {
    favoritePending = false;
  }
}

function callStore() {
  const phone = store.value?.phone;
  if (!phone) return;
  uni.makePhoneCall({ phoneNumber: phone });
}

function openNav() {
  if (!canNavigate.value || !store.value) return;
  openNavigation({
    latitude: Number(store.value.latitude),
    longitude: Number(store.value.longitude),
    name: store.value.name,
    address: store.value.address || "",
  });
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
  storeId = id;
  loadStore(id);
  loadPrices(id);
  loadReviews(id);
  loadFavorite(id);
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

.gallery {
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  background: #e8ecef;

  &__swiper {
    height: 360rpx;
  }

  &__img {
    width: 100%;
    height: 100%;
    display: block;
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

  &__meta {
    margin-top: 8rpx;
    display: flex;
    align-items: center;
    gap: 20rpx;
  }

  &__rating {
    display: flex;
    align-items: center;
    gap: 6rpx;
  }

  &__rating-score {
    font-size: 26rpx;
    font-weight: 700;
    color: #ff8f1f;
  }

  &__rating-count {
    font-size: 22rpx;
    color: #86909c;
  }

  &__dist {
    font-size: 24rpx;
    color: $theme-color;
  }

  &__fav {
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4rpx;
    padding: 8rpx 12rpx;
  }

  &__fav-text {
    font-size: 20rpx;
    color: #86909c;
  }

  &__fav--on .head__fav-text {
    color: #ff4d4f;
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

  &__right {
    text-align: right;
  }

  &__guide {
    margin-top: 4rpx;
    font-size: 20rpx;
    color: #86909c;
  }
}

.review-row {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f7f8fa;

  &:last-child {
    border-bottom: none;
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  &__nick {
    font-size: 24rpx;
    color: #4e5969;
    font-weight: 600;
  }

  &__time {
    margin-left: auto;
    font-size: 22rpx;
    color: #c0c4cc;
  }

  &__comment {
    margin-top: 12rpx;
    font-size: 26rpx;
    color: #4e5969;
    line-height: 1.6;
  }
}

.review-empty {
  text-align: center;
  color: #c0c4cc;
  font-size: 24rpx;
  padding: 24rpx 0;
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
