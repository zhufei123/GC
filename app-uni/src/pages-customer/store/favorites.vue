<template>
  <view class="fav">
    <view v-if="loading" class="fav__loading"><wd-loading color="#07c160" /></view>

    <template v-else>
      <view v-for="s in list" :key="s.id" class="fav-card" @tap="goDetail(s)">
        <view class="fav-card__icon">
          <wd-icon name="home" size="40rpx" color="#07c160" />
        </view>
        <view class="fav-card__info">
          <view class="fav-card__line1">
            <text class="fav-card__name">{{ s.name || "回收站" }}</text>
            <wd-tag v-if="s.available === false" plain>已下线</wd-tag>
            <wd-tag v-else-if="s.businessStatus === 0" plain>休息中</wd-tag>
          </view>
          <view class="fav-card__addr">{{ s.address || "地址待完善" }}</view>
          <view class="fav-card__meta">
            <text v-if="s.businessHours">营业 {{ s.businessHours }}</text>
            <text v-if="s.favoritedAt">{{ s.favoritedAt.slice(0, 10) }} 收藏</text>
          </view>
        </view>
        <view class="fav-card__action">
          <view class="fav-card__unfav" @tap.stop="onUnfavorite(s)">
            <wd-icon name="heart-filled" size="36rpx" color="#ff4d4f" />
          </view>
          <wd-button
            v-if="s.available !== false"
            size="small"
            type="primary"
            plain
            @click.stop="goDetail(s)"
          >
            去看看
          </wd-button>
        </view>
      </view>

      <wd-status-tip v-if="!list.length" image="collect" tip="还没有收藏的回收站">
        <template #bottom>
          <wd-button type="primary" size="small" custom-class="fav__go" @click="goNearby">
            去逛逛附近回收站
          </wd-button>
        </template>
      </wd-status-tip>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getFavoriteStations, unfavoriteStation } from "@/api/store";
import type { FavoriteStationItem } from "@/api/store";

const list = ref<FavoriteStationItem[]>([]);
const loading = ref(true);

async function load() {
  try {
    list.value = (await getFavoriteStations()) || [];
  } catch (e) {
    /* 保持空态 */
  } finally {
    loading.value = false;
  }
}

function goDetail(s: FavoriteStationItem) {
  if (s.available === false) {
    uni.showToast({ title: "该回收站已下线", icon: "none" });
    return;
  }
  uni.navigateTo({ url: `/pages-customer/store/detail?id=${s.id}` });
}

function onUnfavorite(s: FavoriteStationItem) {
  uni.showModal({
    title: "取消收藏",
    content: `不再收藏「${s.name || "该回收站"}」？`,
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await unfavoriteStation(s.id);
        list.value = list.value.filter((it) => it.id !== s.id);
        uni.showToast({ title: "已取消收藏", icon: "none" });
      } catch (e) {
        /* 错误提示已由 request 统一处理 */
      }
    },
  });
}

function goNearby() {
  uni.navigateTo({ url: "/pages-customer/store/nearby" });
}

onShow(() => {
  // 从详情页返回时刷新收藏态
  load();
});
</script>

<style lang="scss" scoped>
.fav {
  min-height: 100vh;
  background: $page-bg;
  padding: 24rpx 32rpx 48rpx;
  box-sizing: border-box;

  &__loading {
    display: flex;
    justify-content: center;
    padding: 120rpx 0;
  }

  :deep(.fav__go) {
    margin-top: 24rpx;
    border-radius: 36rpx !important;
  }
}

.fav-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;

  &__icon {
    width: 80rpx;
    height: 80rpx;
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

  &__line1 {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 700;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__addr {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: #86909c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    margin-top: 8rpx;
    display: flex;
    gap: 20rpx;
    font-size: 22rpx;
    color: #c0c4cc;
  }

  &__action {
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16rpx;
  }

  &__unfav {
    padding: 4rpx 8rpx;
  }
}
</style>
