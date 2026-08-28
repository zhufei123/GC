<template>
  <view class="launch">
    <view class="launch__logo">
      <wd-icon name="refresh" size="72rpx" color="#ffffff" />
    </view>
    <view class="launch__name">绿色回收</view>
    <view class="launch__slogan">让每一件废品都物尽其用</view>
    <view class="launch__loading">
      <wd-loading color="#ffffff" size="40rpx" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from "@dcloudio/uni-app";
import { useUserStore } from "@/store/user";

onLoad(() => {
  const store = useUserStore();
  // App.vue onLaunch 已 restore；此处按登录态与角色分发
  setTimeout(() => {
    if (!store.token) {
      uni.reLaunch({ url: "/pages/login/index" });
    } else {
      uni.reLaunch({ url: store.homePath });
    }
  }, 400);
});
</script>

<style lang="scss" scoped>
.launch {
  min-height: 100vh;
  background: $theme-gradient;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  &__logo {
    width: 160rpx;
    height: 160rpx;
    border-radius: 44rpx;
    background: rgba(255, 255, 255, 0.18);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.12);
  }

  &__name {
    margin-top: 40rpx;
    font-size: 48rpx;
    font-weight: 700;
    color: #fff;
    letter-spacing: 6rpx;
  }

  &__slogan {
    margin-top: 16rpx;
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.85);
  }

  &__loading {
    margin-top: 96rpx;
  }
}
</style>
