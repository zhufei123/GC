<template>
  <view class="search">
    <view class="search__bar">
      <view class="search__input-wrap">
        <wd-icon name="search" size="30rpx" color="#9ca3af" />
        <input
          class="search__input"
          :value="keyword"
          placeholder="搜索废纸 / 金属 / 塑料等品类"
          confirm-type="search"
          focus
          @input="onInput"
          @confirm="doSearch"
        />
        <wd-icon
          v-if="keyword"
          name="close-circle-filled"
          size="30rpx"
          color="#c0c4cc"
          @tap="clearKeyword"
        />
      </view>
      <text class="search__action" @tap="doSearch">搜索</text>
    </view>

    <view class="search__body">
      <view v-if="loading" class="search__loading"><wd-loading color="#07c160" /></view>

      <template v-else-if="searched">
        <view class="search__summary" v-if="results.length">
          共 {{ results.length }} 个相关品类，点击对比附近门店报价
        </view>
        <view v-for="sku in results" :key="sku.id" class="sku-card" @tap="goQuotes(sku)">
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
              <view class="sku-card__guide">指导价</view>
            </template>
            <wd-tag v-else plain>暂无报价</wd-tag>
          </view>
        </view>
        <wd-status-tip
          v-if="!results.length"
          image="search"
          :tip="`未找到「${lastKeyword}」相关品类`"
        />
      </template>

      <view v-else class="search__hint">
        <view class="search__hint-title">热门搜索</view>
        <view class="search__chips">
          <view v-for="w in HOT_WORDS" :key="w" class="search__chip" @tap="searchWord(w)">
            {{ w }}
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { searchSkus } from "@/api/goods";
import type { SkuItem } from "@/api/goods";

const HOT_WORDS = ["废纸", "纸箱", "易拉罐", "塑料瓶", "旧衣服", "废铁", "书本"];

const keyword = ref("");
const lastKeyword = ref("");
const results = ref<SkuItem[]>([]);
const loading = ref(false);
const searched = ref(false);

function onInput(e: any) {
  keyword.value = e.detail.value || "";
}

function clearKeyword() {
  keyword.value = "";
  searched.value = false;
  results.value = [];
}

function searchWord(w: string) {
  keyword.value = w;
  doSearch();
}

async function doSearch() {
  const kw = keyword.value.trim();
  if (!kw) {
    uni.showToast({ title: "请输入搜索关键词", icon: "none" });
    return;
  }
  loading.value = true;
  lastKeyword.value = kw;
  try {
    results.value = (await searchSkus(kw)) || [];
  } catch (e) {
    results.value = [];
  } finally {
    loading.value = false;
    searched.value = true;
  }
}

function goQuotes(sku: SkuItem) {
  uni.navigateTo({
    url: `/pages-customer/store/quotes?skuId=${sku.id}&skuName=${encodeURIComponent(sku.name)}`,
  });
}
</script>

<style lang="scss" scoped>
.search {
  min-height: 100vh;
  background: $page-bg;

  &__bar {
    display: flex;
    align-items: center;
    gap: 20rpx;
    background: #fff;
    padding: 20rpx 32rpx;
  }

  &__input-wrap {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 14rpx;
    background: #f5f6f8;
    height: 72rpx;
    border-radius: 36rpx;
    padding: 0 24rpx;
  }

  &__input {
    flex: 1;
    font-size: 27rpx;
  }

  &__action {
    flex-shrink: 0;
    color: $theme-color;
    font-size: 28rpx;
    font-weight: 600;
  }

  &__body {
    padding: 24rpx 32rpx 48rpx;
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 80rpx 0;
  }

  &__summary {
    font-size: 24rpx;
    color: #86909c;
    margin-bottom: 16rpx;
  }

  &__hint-title {
    font-size: 28rpx;
    font-weight: 700;
    margin-bottom: 20rpx;
  }

  &__chips {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
  }

  &__chip {
    background: #fff;
    border-radius: 32rpx;
    padding: 12rpx 28rpx;
    font-size: 26rpx;
    color: #4e5969;
  }
}

.sku-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;

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
}
</style>
