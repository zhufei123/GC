<template>
  <view class="price">
    <view class="price__header">
      <view class="price__title">今日回收价</view>
      <view class="price__sub">价格实时更新 · 以称重时生效价为准</view>
    </view>

    <view class="price__body">
      <wd-sidebar v-model="activeIndex" custom-class="price__sidebar" @change="onCategoryChange">
        <wd-sidebar-item
          v-for="(cat, i) in categories"
          :key="cat.id"
          :value="i"
          :label="cat.name"
        />
      </wd-sidebar>

      <scroll-view scroll-y class="price__content">
        <view v-if="loading" class="price__loading">
          <wd-loading color="#07c160" />
        </view>
        <template v-else>
          <view v-for="sku in skus" :key="sku.id" class="sku-card" @tap="goQuotes(sku)">
            <view class="sku-card__img">
              <wd-icon name="goods" size="44rpx" color="#07c160" />
            </view>
            <view class="sku-card__info">
              <view class="sku-card__name">{{ sku.name }}</view>
              <view v-if="sku.description" class="sku-card__desc">{{ sku.description }}</view>
              <view class="sku-card__compare">
                门店比价
                <wd-icon name="arrow-right" size="20rpx" color="#07c160" />
              </view>
            </view>
            <view class="sku-card__price">
              <template v-if="sku.price">
                <text class="sku-card__amount">¥{{ sku.price }}</text>
                <text class="sku-card__unit">/{{ sku.unit || "kg" }}</text>
              </template>
              <wd-tag v-else plain>暂无报价</wd-tag>
            </view>
          </view>
          <wd-status-tip v-if="!skus.length" image="content" tip="该分类暂无回收品类" />
        </template>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { getCategoryTree, getSkusUnderCategory } from "@/api/goods";
import type { CategoryNode, SkuItem } from "@/api/goods";

const categories = ref<CategoryNode[]>([]);
const activeIndex = ref(0);
const skus = ref<SkuItem[]>([]);
const loading = ref(false);
let loaded = false;

async function refresh(force = false) {
  if (loaded && !force) return;
  loaded = true;
  loading.value = true;
  try {
    categories.value = (await getCategoryTree()) || [];
    if (categories.value.length) {
      await loadSkus(categories.value[0]);
    }
  } catch (e) {
    /* 空态展示 */
  } finally {
    loading.value = false;
  }
}

async function loadSkus(cat: CategoryNode) {
  loading.value = true;
  try {
    skus.value = await getSkusUnderCategory(cat);
  } catch (e) {
    skus.value = [];
  } finally {
    loading.value = false;
  }
}

function onCategoryChange({ value }: { value: number }) {
  const cat = categories.value[value];
  if (cat) loadSkus(cat);
}

function goQuotes(sku: SkuItem) {
  uni.navigateTo({
    url: `/pages-customer/store/quotes?skuId=${sku.id}&skuName=${encodeURIComponent(sku.name)}`,
  });
}

defineExpose({ refresh });
</script>

<style lang="scss" scoped>
.price {
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

  &__body {
    display: flex;
    height: calc(100vh - 100rpx - 160rpx);
  }

  :deep(.price__sidebar) {
    height: 100%;
  }

  &__content {
    flex: 1;
    height: 100%;
    padding: 20rpx;
    box-sizing: border-box;
    background: #fff;
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 80rpx 0;
  }
}

.sku-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 16rpx;
  border-bottom: 1rpx solid #f2f3f5;

  &__img {
    width: 88rpx;
    height: 88rpx;
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

  &__name {
    font-size: 28rpx;
    font-weight: 600;
  }

  &__desc {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: #86909c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__compare {
    margin-top: 8rpx;
    display: inline-flex;
    align-items: center;
    gap: 4rpx;
    font-size: 22rpx;
    color: $theme-color;
  }

  &__price {
    flex-shrink: 0;
  }

  &__amount {
    color: #ff4d4f;
    font-size: 32rpx;
    font-weight: 700;
  }

  &__unit {
    color: #86909c;
    font-size: 22rpx;
  }
}
</style>
