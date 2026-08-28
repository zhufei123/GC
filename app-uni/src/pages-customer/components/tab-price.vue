<template>
  <view class="price">
    <view class="price__header">
      <view class="price__title">今日回收价</view>
      <view class="price__sub">平台指导价 · 点击品类对比附近回收站报价</view>
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
                <view
                  v-if="sku.trend === 'UP' || sku.trend === 'DOWN'"
                  class="sku-card__trend"
                  :class="sku.trend === 'UP' ? 'sku-card__trend--up' : 'sku-card__trend--down'"
                >
                  {{ sku.trend === "UP" ? "涨" : "跌" }}{{ sku.priceDiff ? ` ${sku.priceDiff}` : "" }}
                </view>
                <view class="sku-card__guide">指导价</view>
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
import { ref, watch } from "vue";
import { getCategoryTree, getSkusUnderCategory } from "@/api/goods";
import type { CategoryNode, SkuItem } from "@/api/goods";

const categories = ref<CategoryNode[]>([]);
const activeIndex = ref(0);
const skus = ref<SkuItem[]>([]);
const loading = ref(false);
let loaded = false;
let loadPromise: Promise<void> | null = null;

function refresh(force = false) {
  if (loaded && !force) return loadPromise;
  loaded = true;
  loadPromise = doLoad();
  return loadPromise;
}

async function doLoad() {
  loading.value = true;
  try {
    categories.value = (await getCategoryTree()) || [];
    const target = categories.value[activeIndex.value] || categories.value[0];
    if (target) {
      await loadSkus(target);
    }
  } catch (e) {
    /* 空态展示 */
  } finally {
    if (!categories.value.length) {
      loaded = false;
      loadPromise = null;
      loading.value = false;
    }
  }
}

/** 首页分类宫格跳转：定位到该分类(含子分类命中)并加载 SKU */
async function selectCategory(categoryId: string | number) {
  await (loadPromise || refresh());
  const id = String(categoryId);
  const index = categories.value.findIndex(
    (c) =>
      String(c.id) === id || (c.children || []).some((child) => String(child.id) === id)
  );
  if (index < 0) return;
  activeIndex.value = index;
  await loadSkus(categories.value[index]);
}

/** 单调递增请求序号：旧请求的响应到达时直接丢弃，避免快速切分类被乱序覆盖 */
let skuLoadSeq = 0;

async function loadSkus(cat: CategoryNode) {
  const seq = ++skuLoadSeq;
  loading.value = true;
  try {
    const list = await getSkusUnderCategory(cat);
    if (seq !== skuLoadSeq) return;
    skus.value = list;
  } catch (e) {
    if (seq !== skuLoadSeq) return;
    skus.value = [];
  } finally {
    if (seq === skuLoadSeq) loading.value = false;
  }
}

function onCategoryChange(ev: { value?: number } | number) {
  const value = typeof ev === "number" ? ev : Number(ev?.value);
  if (Number.isNaN(value)) return;
  const cat = categories.value[value];
  if (cat) loadSkus(cat);
}

watch(activeIndex, (i) => {
  const cat = categories.value[i];
  if (cat) loadSkus(cat);
});

function goQuotes(sku: SkuItem) {
  uni.navigateTo({
    url: `/pages-customer/store/quotes?skuId=${sku.id}&skuName=${encodeURIComponent(sku.name)}`,
  });
}

refresh();
defineExpose({ refresh, selectCategory });
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
    display: flex;
    flex-direction: column;
    align-items: flex-end;
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

  &__guide {
    margin-top: 4rpx;
    font-size: 20rpx;
    color: #86909c;
    text-align: right;
  }

  // 中国股市习惯：涨红跌绿
  &__trend {
    margin-top: 6rpx;
    font-size: 20rpx;
    font-weight: 600;
    padding: 2rpx 12rpx;
    border-radius: 8rpx;

    &--up {
      color: #ff4d4f;
      background: rgba(255, 77, 79, 0.1);
    }

    &--down {
      color: #07c160;
      background: rgba(7, 193, 96, 0.1);
    }
  }
}
</style>
