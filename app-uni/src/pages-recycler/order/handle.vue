<template>
  <view class="handle">
    <!-- 步骤 -->
    <view class="steps">
      <view class="steps__item" :class="{ 'steps__item--active': step === 'weigh' }">
        <view class="steps__dot">1</view>
        <text>称重录入</text>
      </view>
      <view class="steps__line" />
      <view class="steps__item" :class="{ 'steps__item--active': step === 'complete' }">
        <view class="steps__dot">2</view>
        <text>收款完成</text>
      </view>
    </view>

    <!-- 订单摘要 -->
    <view v-if="order" class="card summary">
      <view class="summary__row">
        <text class="summary__label">单号</text>
        <text>{{ order.orderNo || order.id }}</text>
      </view>
      <view class="summary__row">
        <text class="summary__label">客户</text>
        <text>{{ order.receiver || "-" }} {{ order.phone || "" }}</text>
      </view>
      <view class="summary__row">
        <text class="summary__label">地址</text>
        <text class="summary__addr">{{ order.address || "-" }}</text>
      </view>
    </view>

    <!-- 称重步骤 -->
    <template v-if="step === 'weigh'">
      <view class="card">
        <view class="card__title">称重明细</view>
        <view v-for="(row, i) in rows" :key="i" class="weigh-row">
          <view class="weigh-row__info">
            <view class="weigh-row__name">{{ row.skuName }}</view>
            <view class="weigh-row__price">
              {{ row.price ? `¥${row.price}/kg` : "暂无报价" }}
            </view>
          </view>
          <view class="weigh-row__weight">
            <input
              class="weigh-row__input"
              type="digit"
              :value="row.weight"
              placeholder="0.00"
              @input="(e: any) => (row.weight = e.detail.value)"
            />
            <text class="weigh-row__unit">kg</text>
          </view>
          <view class="weigh-row__del" @tap="removeRow(i)">
            <wd-icon name="delete" size="34rpx" color="#c0c4cc" />
          </view>
        </view>

        <view class="add-row" @tap="openSkuPicker">
          <wd-icon name="add-circle" size="34rpx" color="#07c160" />
          <text>添加品类</text>
        </view>
      </view>

      <view class="card">
        <view class="card__title">备注</view>
        <textarea
          v-model="remark"
          class="handle__remark"
          placeholder="称重备注（选填）"
          placeholder-class="handle__remark-placeholder"
          :maxlength="100"
        />
      </view>

      <view class="submit-bar">
        <view>
          <text class="submit-bar__label">合计</text>
          <text class="submit-bar__amount">¥{{ weighTotal }}</text>
        </view>
        <wd-button type="primary" :loading="submitting" custom-class="submit-bar__btn" @click="doWeigh">
          提交称重
        </wd-button>
      </view>
    </template>

    <!-- 收款步骤 -->
    <template v-else>
      <view class="card complete">
        <wd-icon name="wallet" size="88rpx" color="#07c160" />
        <view class="complete__label">应付客户金额（线下现金/转账）</view>
        <view class="complete__amount">¥{{ lockedAmount }}</view>
        <view class="complete__hint">金额已按称重时生效价锁定</view>
        <wd-button
          type="primary"
          block
          size="large"
          :loading="submitting"
          custom-class="complete__btn"
          @click="doComplete"
        >
          确认已线下付款，完成订单
        </wd-button>
      </view>
    </template>

    <!-- SKU 选择 -->
    <wd-action-sheet v-model="skuPickerVisible" title="选择回收品类">
      <view class="sku-picker">
        <view
          v-for="sku in availableSkus"
          :key="sku.id"
          class="sku-picker__item"
          @tap="addSku(sku)"
        >
          <text>{{ sku.name }}</text>
          <text class="sku-picker__price">{{ sku.price ? `¥${sku.price}/kg` : "暂无报价" }}</text>
        </view>
        <view v-if="!availableSkus.length" class="sku-picker__empty">暂无可选品类</view>
      </view>
    </wd-action-sheet>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import {
  getWeighInit,
  getBossOrderDetail,
  getAvailableSkus,
  submitWeigh,
  completeOrder,
} from "@/api/boss";
import type { OrderVO } from "@/api/order";

interface WeighRow {
  skuId: string;
  skuName: string;
  price?: string;
  weight: string;
}

const order = ref<OrderVO | null>(null);
const step = ref<"weigh" | "complete">("weigh");
const rows = ref<WeighRow[]>([]);
const remark = ref("");
const submitting = ref(false);
const lockedAmount = ref("0.00");
const skuPickerVisible = ref(false);
const availableSkus = ref<Array<{ id: string; name: string; price?: string }>>([]);
let orderId = "";

const weighTotal = computed(() => {
  let total = 0;
  for (const row of rows.value) {
    const p = parseFloat(row.price || "0");
    const w = parseFloat(row.weight || "0");
    if (p > 0 && w > 0) total += p * w;
  }
  return total.toFixed(2);
});

async function load() {
  let data: OrderVO | null = null;
  try {
    data = await getWeighInit(orderId);
  } catch (e) {
    /* weigh-init 不可用时降级订单详情 */
  }
  if (!data) {
    try {
      data = await getBossOrderDetail(orderId);
    } catch (e) {
      return;
    }
  }
  order.value = data;
  if (data.status === "WEIGHED") {
    step.value = "complete";
    lockedAmount.value = data.actualAmount || "0.00";
    return;
  }
  rows.value = (data.items || []).map((it) => ({
    skuId: it.skuId,
    skuName: it.skuName || it.skuId,
    price: it.price,
    weight: it.weight || it.estimateWeight || "",
  }));
}

async function openSkuPicker() {
  if (!availableSkus.value.length) {
    try {
      availableSkus.value = (await getAvailableSkus()) || [];
    } catch (e) {
      availableSkus.value = [];
    }
  }
  skuPickerVisible.value = true;
}

function addSku(sku: { id: string; name: string; price?: string }) {
  if (!rows.value.some((r) => r.skuId === sku.id)) {
    rows.value.push({ skuId: sku.id, skuName: sku.name, price: sku.price, weight: "" });
  }
  skuPickerVisible.value = false;
}

function removeRow(i: number) {
  rows.value.splice(i, 1);
}

async function doWeigh() {
  const items = rows.value
    .filter((r) => parseFloat(r.weight || "0") > 0)
    .map((r) => ({ skuId: r.skuId, weight: String(parseFloat(r.weight)) }));
  if (!items.length) {
    uni.showToast({ title: "请录入至少一项重量", icon: "none" });
    return;
  }
  submitting.value = true;
  try {
    const res: any = await submitWeigh(orderId, { items, images: [], remark: remark.value });
    lockedAmount.value = res?.actualAmount || res?.totalAmount || weighTotal.value;
    // 以服务端锁定金额为准
    try {
      const fresh = await getBossOrderDetail(orderId);
      if (fresh?.actualAmount) lockedAmount.value = fresh.actualAmount;
    } catch (e) {
      /* 保持当前金额 */
    }
    step.value = "complete";
    uni.showToast({ title: "称重已提交", icon: "success" });
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    submitting.value = false;
  }
}

async function doComplete() {
  submitting.value = true;
  try {
    await completeOrder(orderId, lockedAmount.value);
    uni.showToast({ title: "订单已完成", icon: "success" });
    setTimeout(() => {
      uni.reLaunch({ url: "/pages-recycler/index?tab=2" });
    }, 800);
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    submitting.value = false;
  }
}

onLoad((options) => {
  orderId = options?.id || "";
  if (orderId) load();
});
</script>

<style lang="scss" scoped>
.handle {
  padding: 24rpx 32rpx 200rpx;

  &__remark {
    width: 100%;
    height: 120rpx;
    background: #f7f8fa;
    border-radius: 16rpx;
    padding: 20rpx;
    box-sizing: border-box;
    font-size: 26rpx;
  }

  &__remark-placeholder {
    color: #c0c4cc;
  }
}

.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24rpx;
  padding: 24rpx 0 32rpx;

  &__item {
    display: flex;
    align-items: center;
    gap: 12rpx;
    color: #86909c;
    font-size: 27rpx;

    &--active {
      color: $theme-color;
      font-weight: 700;

      .steps__dot {
        background: $theme-color;
        color: #fff;
      }
    }
  }

  &__dot {
    width: 44rpx;
    height: 44rpx;
    border-radius: 50%;
    background: #e5e6eb;
    color: #86909c;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24rpx;
  }

  &__line {
    width: 80rpx;
    height: 2rpx;
    background: #e5e6eb;
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
    margin-bottom: 20rpx;
  }
}

.summary {
  &__row {
    display: flex;
    gap: 24rpx;
    padding: 8rpx 0;
    font-size: 26rpx;
  }

  &__label {
    color: #86909c;
    flex-shrink: 0;
  }

  &__addr {
    flex: 1;
  }
}

.weigh-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f2f3f5;

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 28rpx;
    font-weight: 600;
  }

  &__price {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: #ff4d4f;
  }

  &__weight {
    display: flex;
    align-items: center;
    gap: 8rpx;
    background: #f7f8fa;
    border-radius: 12rpx;
    padding: 12rpx 16rpx;
  }

  &__input {
    width: 120rpx;
    font-size: 28rpx;
    text-align: right;
  }

  &__unit {
    font-size: 24rpx;
    color: #86909c;
  }

  &__del {
    padding: 8rpx;
  }
}

.add-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  padding: 24rpx 0 8rpx;
  color: $theme-color;
  font-size: 28rpx;
  font-weight: 600;
}

.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -6rpx 20rpx rgba(31, 35, 41, 0.06);

  &__label {
    font-size: 24rpx;
    color: #86909c;
    margin-right: 12rpx;
  }

  &__amount {
    font-size: 40rpx;
    font-weight: 700;
    color: #ff4d4f;
  }

  :deep(.submit-bar__btn) {
    border-radius: 44rpx !important;
    padding: 0 64rpx !important;
  }
}

.complete {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64rpx 40rpx;

  &__label {
    margin-top: 28rpx;
    font-size: 26rpx;
    color: #86909c;
  }

  &__amount {
    margin-top: 16rpx;
    font-size: 72rpx;
    font-weight: 700;
    color: #ff4d4f;
  }

  &__hint {
    margin-top: 12rpx;
    font-size: 22rpx;
    color: #c0c4cc;
  }

  :deep(.complete__btn) {
    margin-top: 56rpx;
    border-radius: 48rpx !important;
    width: 100%;
  }
}

.sku-picker {
  max-height: 60vh;
  overflow-y: auto;
  padding: 0 32rpx 40rpx;

  &__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 28rpx 8rpx;
    border-bottom: 1rpx solid #f2f3f5;
    font-size: 28rpx;
  }

  &__price {
    color: #ff4d4f;
    font-size: 26rpx;
  }

  &__empty {
    text-align: center;
    color: #c0c4cc;
    padding: 48rpx 0;
    font-size: 26rpx;
  }
}
</style>
