<template>
  <view class="bprice">
    <view class="bprice__hint">
      <wd-icon name="info-circle" size="30rpx" color="#ff8f1f" />
      <text>指导价仅供参考，实际回收价由本站发布，用户按距离和报价选站。</text>
    </view>

    <view v-if="loading" class="bprice__loading"><wd-loading color="#07c160" /></view>
    <template v-else>
      <!-- 批量调价工具栏(修改后仍需点击保存生效) -->
      <view v-if="items.length" class="bulk-bar">
        <view class="bulk-bar__btn" @tap="syncGuidePrices">同步指导价</view>
        <view class="bulk-bar__btn" @tap="adjustAll(5)">全部上调5%</view>
        <view class="bulk-bar__btn" @tap="adjustAll(-5)">全部下调5%</view>
      </view>

      <view v-for="item in items" :key="item.skuId" class="price-card">
        <view class="price-card__main">
          <view class="price-card__info">
            <view class="price-card__name">
              <text>{{ item.skuName }}</text>
              <text v-if="item.categoryName" class="price-card__cate">{{ item.categoryName }}</text>
            </view>
            <view class="price-card__guide">
              指导价 {{ item.guidePrice ? `¥${item.guidePrice}/${item.unit || "kg"}` : "暂无" }}
            </view>
          </view>
          <view class="price-card__switch">
            <wd-switch
              :model-value="item.active"
              size="44rpx"
              @change="({ value }: any) => (item.active = value)"
            />
            <text
              class="price-card__switch-label"
              :class="{ 'price-card__switch-label--off': !item.active }"
            >
              {{ item.active ? "报价中" : "停报" }}
            </text>
          </view>
        </view>
        <view class="price-card__quote" :class="{ 'price-card__quote--off': !item.active }">
          <text class="price-card__label">本站价</text>
          <view class="price-card__input-wrap">
            <text class="price-card__yen">¥</text>
            <input
              class="price-card__input"
              type="digit"
              :value="item.price"
              :disabled="!item.active"
              placeholder="0.00"
              @input="(e: any) => (item.price = e.detail.value)"
            />
            <text class="price-card__unit">/{{ item.unit || "kg" }}</text>
          </view>
        </view>
      </view>

      <wd-status-tip v-if="!items.length" image="content" tip="暂无可报价品类" />
    </template>

    <view v-if="items.length" class="save-bar">
      <wd-button
        type="primary"
        block
        size="large"
        :loading="saving"
        custom-class="save-bar__btn"
        @click="onSave"
      >
        保存报价
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { getStationPrices, updateStationPrices, createStationPrices } from "@/api/boss";
import type { StationPriceItem } from "@/api/boss";
import { getSkuList } from "@/api/goods";

interface PriceRow {
  skuId: string;
  skuName: string;
  unit?: string;
  categoryName?: string;
  guidePrice?: string;
  price: string;
  active: boolean;
}

const items = ref<PriceRow[]>([]);
const loading = ref(false);
const saving = ref(false);
/** GET /boss/prices 是否可用；不可用时保存降级走 POST */
let apiReady = true;

function toRow(it: StationPriceItem): PriceRow {
  return {
    skuId: it.skuId,
    skuName: it.skuName || it.skuId,
    unit: it.unit,
    categoryName: it.categoryName,
    guidePrice: it.guidePrice,
    price: it.price != null && it.price !== "" ? String(it.price) : "",
    active: typeof it.status === "undefined" ? true : Number(it.status) === 1,
  };
}

async function load() {
  loading.value = true;
  try {
    const res = await getStationPrices();
    apiReady = true;
    const rows = res?.list || [];
    if (rows.length) {
      items.value = rows.map(toRow);
      return;
    }
    // 接口可用但尚无报价：用平台 SKU 兜底，首次填价后保存
    await loadFromSkus();
  } catch (e) {
    // 报价接口未就绪：SKU 平台价视为指导价，本地编辑后 POST 发布
    apiReady = false;
    await loadFromSkus();
  } finally {
    loading.value = false;
  }
}

async function loadFromSkus() {
  try {
    const skus = (await getSkuList()) || [];
    items.value = skus.map((s) => ({
      skuId: s.id,
      skuName: s.name,
      unit: s.unit,
      guidePrice: s.price || undefined,
      price: s.price ? String(s.price) : "",
      active: true,
    }));
  } catch (e) {
    items.value = [];
  }
}

/** 指导价复制为本站价(所有有指导价的行)，仍需点击保存生效 */
function syncGuidePrices() {
  let count = 0;
  for (const it of items.value) {
    if (it.guidePrice && parseFloat(it.guidePrice) > 0) {
      it.price = String(it.guidePrice);
      count += 1;
    }
  }
  uni.showToast({
    title: count ? `已同步 ${count} 项，请点击保存` : "暂无可同步的指导价",
    icon: "none",
  });
}

/** 全部报价按百分比上调/下调(保留2位小数)，仍需点击保存生效 */
function adjustAll(percent: number) {
  let count = 0;
  for (const it of items.value) {
    const n = parseFloat(it.price || "");
    if (!Number.isNaN(n) && n > 0) {
      it.price = (Math.round(n * (1 + percent / 100) * 100) / 100).toFixed(2);
      count += 1;
    }
  }
  uni.showToast({
    title: count
      ? `已${percent > 0 ? "上调" : "下调"} ${count} 项，请点击保存`
      : "暂无可调整的报价",
    icon: "none",
  });
}

async function onSave() {
  const invalid = items.value.find((it) => it.active && !(parseFloat(it.price || "0") > 0));
  if (invalid) {
    uni.showToast({ title: `请填写「${invalid.skuName}」的本站价`, icon: "none" });
    return;
  }
  const payload = items.value.map((it) => ({
    skuId: it.skuId,
    price: parseFloat(it.price || "0") > 0 ? it.price.trim() : "0",
    status: it.active ? 1 : 0,
  }));
  saving.value = true;
  try {
    if (apiReady) {
      await updateStationPrices(payload);
    } else {
      await createStationPrices(payload);
    }
    uni.showToast({ title: "报价已保存", icon: "success" });
    load();
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    saving.value = false;
  }
}

load();
</script>

<style lang="scss" scoped>
.bprice {
  padding: 24rpx 32rpx 200rpx;

  &__hint {
    display: flex;
    align-items: flex-start;
    gap: 12rpx;
    background: #fff7e8;
    border-radius: 16rpx;
    padding: 20rpx 24rpx;
    margin-bottom: 24rpx;
    font-size: 24rpx;
    color: #ff8f1f;
    line-height: 1.5;
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 120rpx 0;
  }
}

.bulk-bar {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;

  &__btn {
    flex: 1;
    text-align: center;
    background: #fff;
    border: 2rpx solid $theme-color;
    color: $theme-color;
    font-size: 25rpx;
    font-weight: 600;
    border-radius: 40rpx;
    padding: 14rpx 0;
  }
}

.price-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;

  &__main {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20rpx;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    display: flex;
    align-items: center;
    gap: 12rpx;
    font-size: 30rpx;
    font-weight: 600;
  }

  &__cate {
    font-size: 20rpx;
    font-weight: 400;
    color: #07c160;
    background: $theme-color-light;
    border-radius: 8rpx;
    padding: 4rpx 12rpx;
    flex-shrink: 0;
  }

  &__guide {
    margin-top: 10rpx;
    font-size: 24rpx;
    color: #86909c;
  }

  &__switch {
    display: flex;
    align-items: center;
    gap: 12rpx;
    flex-shrink: 0;
  }

  &__switch-label {
    font-size: 24rpx;
    color: $theme-color;
    width: 76rpx;

    &--off {
      color: #c0c4cc;
    }
  }

  &__quote {
    margin-top: 24rpx;
    padding-top: 24rpx;
    border-top: 1rpx solid #f2f3f5;
    display: flex;
    align-items: center;
    justify-content: space-between;

    &--off {
      opacity: 0.4;
    }
  }

  &__label {
    font-size: 26rpx;
    color: #4e5969;
  }

  &__input-wrap {
    display: flex;
    align-items: center;
    gap: 8rpx;
    background: #f7f8fa;
    border-radius: 12rpx;
    padding: 14rpx 20rpx;
  }

  &__yen {
    font-size: 26rpx;
    color: #ff4d4f;
    font-weight: 600;
  }

  &__input {
    width: 160rpx;
    font-size: 30rpx;
    font-weight: 600;
    text-align: right;
  }

  &__unit {
    font-size: 24rpx;
    color: #86909c;
  }
}

.save-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -6rpx 20rpx rgba(31, 35, 41, 0.06);

  :deep(.save-bar__btn) {
    border-radius: 48rpx !important;
  }
}
</style>
