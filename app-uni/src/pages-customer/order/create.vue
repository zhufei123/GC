<template>
  <view class="create">
    <!-- 回收方式 -->
    <view class="card">
      <view class="card__title">回收方式</view>
      <view class="chips">
        <view
          class="chips__item"
          :class="{ 'chips__item--active': orderType === 'PICKUP' }"
          @tap="orderType = 'PICKUP'"
        >
          上门回收
        </view>
        <view
          class="chips__item"
          :class="{ 'chips__item--active': orderType === 'DROPOFF' }"
          @tap="orderType = 'DROPOFF'"
        >
          自行送货
        </view>
      </view>
    </view>

    <!-- 回收站 -->
    <view class="card address" @tap="chooseStore">
      <view class="address__icon">
        <wd-icon name="home" size="40rpx" color="#07c160" />
      </view>
      <view v-if="store" class="address__info">
        <view class="address__line1">
          <text class="address__name">{{ store.name }}</text>
          <wd-tag type="success" plain>已选回收站</wd-tag>
        </view>
        <view v-if="store.address" class="address__line2">{{ store.address }}</view>
      </view>
      <view v-else class="address__empty">请选择回收站</view>
      <wd-icon name="arrow-right" size="30rpx" color="#c0c4cc" />
    </view>

    <!-- 地址(仅上门回收) -->
    <view v-if="orderType === 'PICKUP'" class="card address" @tap="chooseAddress">
      <view class="address__icon">
        <wd-icon name="location" size="40rpx" color="#07c160" />
      </view>
      <view v-if="address" class="address__info">
        <view class="address__line1">
          <text class="address__name">{{ address.receiver }}</text>
          <text class="address__phone">{{ address.phone }}</text>
        </view>
        <view class="address__line2">
          {{ address.province }}{{ address.city }}{{ address.district }}{{ address.street || "" }}{{ address.detail }}
        </view>
      </view>
      <view v-else class="address__empty">请选择上门地址</view>
      <wd-icon name="arrow-right" size="30rpx" color="#c0c4cc" />
    </view>

    <!-- 回收品类 -->
    <view class="card">
      <view class="card__title">
        选择回收品类
        <wd-tag v-if="priceSource === 'store'" type="success" plain custom-class="card__title-tag">
          门店报价
        </wd-tag>
        <wd-tag v-else plain custom-class="card__title-tag">指导价</wd-tag>
      </view>
      <view v-if="skuLoading" class="create__loading"><wd-loading color="#07c160" /></view>
      <view v-else class="sku-select">
        <view
          v-for="sku in skus"
          :key="sku.id"
          class="sku-select__item"
          :class="{ 'sku-select__item--active': !!selected[sku.id] }"
          @tap="toggleSku(sku)"
        >
          <view class="sku-select__name">{{ sku.name }}</view>
          <view class="sku-select__price">
            {{ sku.price ? `¥${sku.price}/${sku.unit || "kg"}` : "暂无报价" }}
          </view>
          <view v-if="selected[sku.id]" class="sku-select__check">
            <wd-icon name="check-bold" size="22rpx" color="#ffffff" />
          </view>
        </view>
        <wd-status-tip v-if="!skus.length" image="content" tip="暂无可回收品类" />
      </view>

      <view v-if="selectedList.length" class="weight-list">
        <view class="weight-list__title">预估重量（kg）</view>
        <view v-for="sku in selectedList" :key="sku.id" class="weight-list__row">
          <text class="weight-list__name">{{ sku.name }}</text>
          <wd-input-number
            :model-value="selected[sku.id]"
            :min="0.5"
            :max="999"
            :step="0.5"
            input-width="100rpx"
            @change="({ value }: any) => (selected[sku.id] = value)"
          />
        </view>
      </view>
    </view>

    <!-- 预约时间 -->
    <view class="card">
      <view class="card__title">{{ orderType === "PICKUP" ? "预约上门时间" : "预计送达时间" }}</view>
      <view class="chips">
        <view
          v-for="d in dateOptions"
          :key="d.value"
          class="chips__item"
          :class="{ 'chips__item--active': appointDate === d.value }"
          @tap="appointDate = d.value"
        >
          {{ d.label }}
        </view>
      </view>
      <view class="chips" style="margin-top: 20rpx">
        <view
          v-for="p in periods"
          :key="p"
          class="chips__item"
          :class="{ 'chips__item--active': appointPeriod === p }"
          @tap="appointPeriod = p"
        >
          {{ p }}
        </view>
      </view>
    </view>

    <!-- 备注 -->
    <view class="card">
      <view class="card__title">备注</view>
      <textarea
        v-model="remark"
        class="create__remark"
        placeholder="如：纸箱较多，请带小推车（选填）"
        placeholder-class="create__remark-placeholder"
        :maxlength="100"
      />
    </view>

    <!-- 底部提交栏 -->
    <view class="submit-bar">
      <view class="submit-bar__left">
        <text class="submit-bar__label">预估可得</text>
        <text class="submit-bar__amount">¥{{ estimateTotal }}</text>
      </view>
      <wd-button type="primary" :loading="submitting" custom-class="submit-bar__btn" @click="submit">
        提交预约
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from "vue";
import { onLoad, onUnload, onShow } from "@dcloudio/uni-app";
import { getAllSkus } from "@/api/goods";
import type { SkuItem } from "@/api/goods";
import { getAddressList } from "@/api/address";
import type { AddressItem } from "@/api/address";
import { createOrder, getTimeslots } from "@/api/order";
import { getStorePrices, getCachedStore } from "@/api/store";
import type { StoreItem } from "@/api/store";

const DEFAULT_PERIODS = ["09:00-11:00", "11:00-13:00", "14:00-16:00", "16:00-18:00"];

const address = ref<AddressItem | null>(null);
const store = ref<StoreItem | null>(null);
const orderType = ref<"PICKUP" | "DROPOFF">("PICKUP");
/** store=门店报价 guide=平台指导价 */
const priceSource = ref<"store" | "guide">("guide");
const skus = ref<SkuItem[]>([]);
const skuLoading = ref(true);
/** skuId -> 预估重量 */
const selected = reactive<Record<string, number>>({});
const periods = ref<string[]>(DEFAULT_PERIODS);
const appointDate = ref("");
const appointPeriod = ref("");
const remark = ref("");
const submitting = ref(false);

const dateOptions = buildDateOptions();
appointDate.value = dateOptions[0].value;

function buildDateOptions() {
  const labels = ["今天", "明天", "后天"];
  return labels.map((label, i) => {
    const d = new Date(Date.now() + i * 86400000);
    const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
      d.getDate()
    ).padStart(2, "0")}`;
    return { label: `${label} ${value.slice(5)}`, value };
  });
}

const selectedList = computed(() => skus.value.filter((s) => !!selected[s.id]));

const estimateTotal = computed(() => {
  let total = 0;
  for (const sku of selectedList.value) {
    const price = parseFloat(sku.price || "0");
    if (price > 0) total += price * (selected[sku.id] || 0);
  }
  return total.toFixed(2);
});

function toggleSku(sku: SkuItem) {
  if (selected[sku.id]) {
    delete selected[sku.id];
  } else {
    selected[sku.id] = 5;
  }
}

function chooseAddress() {
  uni.navigateTo({ url: "/pages-customer/address/list?select=1" });
}

function chooseStore() {
  uni.navigateTo({ url: "/pages-customer/store/nearby?select=1" });
}

/** 门店价目就绪时覆盖指导价；404/为空则维持 sku/list 指导价 */
async function applyStorePrices() {
  if (!store.value?.id) {
    priceSource.value = "guide";
    return;
  }
  try {
    const list = await getStorePrices(String(store.value.id));
    // status=0 停报的条目不参与覆盖
    const active = (list || []).filter((p) => p.status !== 0);
    if (!active.length) throw new Error("empty store prices");
    const priceMap = new Map(active.map((p) => [String(p.skuId), p]));
    skus.value = skus.value.map((s) => {
      const p = priceMap.get(String(s.id));
      return p ? { ...s, price: p.price != null ? String(p.price) : s.price, unit: p.unit || s.unit } : s;
    });
    priceSource.value = "store";
  } catch (e) {
    priceSource.value = "guide";
  }
}

async function loadData() {
  skuLoading.value = true;
  try {
    const { skus: all } = await getAllSkus();
    skus.value = all;
    await applyStorePrices();
  } catch (e) {
    skus.value = [];
  } finally {
    skuLoading.value = false;
  }
  try {
    const slots = await getTimeslots();
    if (Array.isArray(slots) && slots.length) {
      const first = slots[0] as any;
      if (typeof first === "string") {
        periods.value = slots as string[];
      } else {
        const match =
          (slots as any[]).find((s) => s.date === appointDate.value) || first;
        if (Array.isArray(match?.periods) && match.periods.length) {
          periods.value = match.periods;
        }
      }
    }
  } catch (e) {
    /* 使用默认时段 */
  }
  if (!address.value) {
    try {
      const list = (await getAddressList()) || [];
      address.value = list.find((a) => a.isDefault === true || Number(a.isDefault) === 1) || list[0] || null;
    } catch (e) {
      /* 未选地址提示用户手动选择 */
    }
  }
}

async function submit() {
  if (!store.value?.id) {
    uni.showToast({ title: "请先选择回收站", icon: "none" });
    chooseStore();
    return;
  }
  if (orderType.value === "PICKUP" && !address.value?.id) {
    uni.showToast({ title: "请选择上门地址", icon: "none" });
    return;
  }
  if (!selectedList.value.length) {
    uni.showToast({ title: "请选择回收品类", icon: "none" });
    return;
  }
  if (!appointPeriod.value) {
    uni.showToast({ title: "请选择时间段", icon: "none" });
    return;
  }
  submitting.value = true;
  try {
    await createOrder({
      type: orderType.value,
      storeId: String(store.value.id),
      ...(orderType.value === "PICKUP" && address.value?.id
        ? { addressId: String(address.value.id) }
        : {}),
      appointDate: appointDate.value,
      appointPeriod: appointPeriod.value,
      estimateItems: selectedList.value.map((s) => ({
        skuId: s.id,
        estimateWeight: String(selected[s.id]),
      })),
      images: [],
      remark: remark.value,
      requestId: `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`,
    });
    uni.showToast({ title: "预约成功", icon: "success" });
    setTimeout(() => {
      uni.reLaunch({ url: "/pages-customer/index?tab=2" });
    }, 800);
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    submitting.value = false;
  }
}

onLoad((options) => {
  if (options?.type === "DROPOFF" || options?.type === "PICKUP") {
    orderType.value = options.type;
  }
  const storeId = options?.storeId ? String(options.storeId) : "";
  if (storeId) {
    const cached = getCachedStore(storeId);
    store.value =
      cached ||
      ({
        id: storeId,
        name: options?.storeName ? decodeURIComponent(String(options.storeName)) : `回收站 ${storeId}`,
      } as StoreItem);
  }
  uni.$on("address-selected", (item: AddressItem) => {
    address.value = item;
  });
  uni.$on("store-selected", (item: StoreItem) => {
    store.value = item;
    applyStorePrices();
  });
  loadData();
});

onShow(() => {
  /* 从地址页返回时若删除了当前地址，保持提示即可 */
});

onUnload(() => {
  uni.$off("address-selected");
  uni.$off("store-selected");
});
</script>

<style lang="scss" scoped>
.create {
  padding: 24rpx 32rpx 200rpx;

  &__loading {
    display: flex;
    justify-content: center;
    padding: 48rpx 0;
  }

  &__remark {
    width: 100%;
    height: 140rpx;
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

.address {
  display: flex;
  align-items: center;
  gap: 20rpx;

  &__icon {
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
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
    gap: 20rpx;
    align-items: baseline;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 700;
  }

  &__phone {
    font-size: 24rpx;
    color: #86909c;
  }

  &__line2 {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: #4e5969;
  }

  &__empty {
    flex: 1;
    color: #86909c;
    font-size: 28rpx;
  }
}

.sku-select {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;

  &__item {
    position: relative;
    width: calc(50% - 10rpx);
    box-sizing: border-box;
    border: 2rpx solid #ebedf0;
    border-radius: 16rpx;
    padding: 20rpx;
    overflow: hidden;

    &--active {
      border-color: $theme-color;
      background: $theme-color-light;
    }
  }

  &__name {
    font-size: 26rpx;
    font-weight: 600;
  }

  &__price {
    margin-top: 8rpx;
    font-size: 22rpx;
    color: #ff4d4f;
  }

  &__check {
    position: absolute;
    right: 0;
    top: 0;
    width: 40rpx;
    height: 40rpx;
    background: $theme-color;
    border-radius: 0 0 0 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.weight-list {
  margin-top: 28rpx;
  border-top: 1rpx solid #f2f3f5;
  padding-top: 24rpx;

  &__title {
    font-size: 26rpx;
    color: #86909c;
    margin-bottom: 16rpx;
  }

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14rpx 0;
  }

  &__name {
    font-size: 28rpx;
  }
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;

  &__item {
    padding: 14rpx 28rpx;
    border-radius: 32rpx;
    background: #f7f8fa;
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
</style>
