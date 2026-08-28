<template>
  <view class="addr-list">
    <view v-if="selectMode" class="addr-list__tip">
      <wd-icon name="info-circle" size="28rpx" color="#07c160" />
      <text>点击选择上门地址</text>
    </view>

    <view
      v-for="item in list"
      :key="item.id"
      class="addr-card"
      @tap="onSelect(item)"
    >
      <view class="addr-card__main">
        <view class="addr-card__line1">
          <text class="addr-card__name">{{ item.receiver }}</text>
          <text class="addr-card__phone">{{ item.phone }}</text>
          <wd-tag v-if="item.isDefault" type="success" plain>默认</wd-tag>
        </view>
        <view class="addr-card__line2">
          {{ item.province }}{{ item.city }}{{ item.district }}{{ item.street || "" }}{{ item.detail }}
        </view>
      </view>
      <view class="addr-card__edit" @tap.stop="onEdit(item)">
        <wd-icon name="edit" size="36rpx" color="#86909c" />
      </view>
    </view>

    <wd-status-tip
      v-if="!loading && !list.length"
      image="content"
      tip="还没有地址，点击下方按钮新增"
    />
    <view v-if="loading" class="addr-list__loading"><wd-loading color="#07c160" /></view>

    <view class="addr-list__footer">
      <wd-button type="primary" block custom-class="addr-list__add" @click="onAdd">
        新增地址
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import { getAddressList } from "@/api/address";
import type { AddressItem } from "@/api/address";

const list = ref<AddressItem[]>([]);
const loading = ref(false);
const selectMode = ref(false);

async function load() {
  loading.value = true;
  try {
    list.value = (await getAddressList()) || [];
  } catch (e) {
    list.value = [];
  } finally {
    loading.value = false;
  }
}

function onSelect(item: AddressItem) {
  if (!selectMode.value) return;
  uni.$emit("address-selected", item);
  uni.navigateBack();
}

function onEdit(item: AddressItem) {
  uni.navigateTo({
    url: `/pages-customer/address/edit?data=${encodeURIComponent(JSON.stringify(item))}`,
  });
}

function onAdd() {
  uni.navigateTo({ url: "/pages-customer/address/edit" });
}

onLoad((options) => {
  selectMode.value = options?.select === "1";
});

onShow(() => {
  load();
});
</script>

<style lang="scss" scoped>
.addr-list {
  padding: 24rpx 32rpx 200rpx;

  &__tip {
    display: flex;
    align-items: center;
    gap: 10rpx;
    background: $theme-color-light;
    color: #07c160;
    font-size: 26rpx;
    border-radius: 16rpx;
    padding: 18rpx 24rpx;
    margin-bottom: 24rpx;
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 48rpx 0;
  }

  &__footer {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
    background: #fff;
    box-shadow: 0 -6rpx 20rpx rgba(31, 35, 41, 0.06);
  }

  :deep(.addr-list__add) {
    border-radius: 44rpx !important;
  }
}

.addr-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__line1 {
    display: flex;
    align-items: center;
    gap: 16rpx;
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
    margin-top: 12rpx;
    font-size: 25rpx;
    color: #4e5969;
  }

  &__edit {
    padding: 16rpx 0 16rpx 24rpx;
    flex-shrink: 0;
  }
}
</style>
