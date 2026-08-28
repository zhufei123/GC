<template>
  <view class="profile">
    <view class="card">
      <view class="avatar-row" @tap="changeAvatar">
        <text class="avatar-row__label">头像</text>
        <view class="avatar-row__right">
          <image v-if="avatar" :src="avatar" mode="aspectFill" class="avatar-row__img" />
          <view v-else class="avatar-row__placeholder">
            <wd-icon name="user-avatar" size="52rpx" color="#07c160" />
          </view>
          <wd-icon name="arrow-right" size="30rpx" color="#c0c4cc" />
        </view>
      </view>

      <view class="field-row">
        <text class="field-row__label">昵称</text>
        <input
          v-model="nickname"
          class="field-row__input"
          :maxlength="32"
          placeholder="请输入昵称"
          placeholder-class="field-row__placeholder"
        />
      </view>

      <view class="field-row">
        <text class="field-row__label">手机号</text>
        <text class="field-row__value">{{ phone || "未绑定" }}</text>
      </view>
    </view>

    <wd-button
      type="primary"
      block
      size="large"
      :loading="saving"
      custom-class="profile__save"
      @click="save"
    >
      保 存
    </wd-button>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getMe } from "@/api/auth";
import { updateProfile } from "@/api/user";
import { useUserStore } from "@/store/user";
import { chooseAndUpload } from "@/utils/upload";

const userStore = useUserStore();

const nickname = ref("");
const avatar = ref("");
const phone = ref("");
const saving = ref(false);

async function load() {
  try {
    const me = await getMe();
    nickname.value = me?.nickname || "";
    avatar.value = me?.avatar || "";
    phone.value = me?.phone || "";
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  }
}

async function changeAvatar() {
  const urls = await chooseAndUpload("avatar", 1);
  if (urls.length) avatar.value = urls[0];
}

async function save() {
  if (!nickname.value.trim()) {
    uni.showToast({ title: "请输入昵称", icon: "none" });
    return;
  }
  saving.value = true;
  try {
    await updateProfile({ nickname: nickname.value.trim(), avatar: avatar.value || undefined });
    userStore.nickname = nickname.value.trim();
    userStore.persist();
    uni.showToast({ title: "已保存", icon: "success" });
    setTimeout(() => uni.navigateBack(), 600);
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    saving.value = false;
  }
}

onLoad(load);
</script>

<style lang="scss" scoped>
.profile {
  min-height: 100vh;
  background: $page-bg;
  padding: 24rpx 32rpx;

  :deep(.profile__save) {
    margin-top: 48rpx;
    border-radius: 48rpx !important;
  }
}

.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 8rpx 28rpx;
}

.avatar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f2f3f5;

  &__label {
    font-size: 29rpx;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  &__img {
    width: 96rpx;
    height: 96rpx;
    border-radius: 50%;
  }

  &__placeholder {
    width: 96rpx;
    height: 96rpx;
    border-radius: 50%;
    background: #e8f9ef;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.field-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f2f3f5;

  &:last-child {
    border-bottom: none;
  }

  &__label {
    font-size: 29rpx;
    flex-shrink: 0;
    width: 120rpx;
  }

  &__input {
    flex: 1;
    font-size: 29rpx;
    text-align: right;
  }

  &__placeholder {
    color: #c0c4cc;
  }

  &__value {
    flex: 1;
    text-align: right;
    font-size: 29rpx;
    color: #86909c;
  }
}
</style>
