<template>
  <view class="login">
    <view class="login__hero">
      <view class="login__logo">
        <wd-icon name="refresh" size="56rpx" color="#ffffff" />
      </view>
      <view class="login__title">绿色回收</view>
      <view class="login__subtitle">上门回收 · 环保变现</view>
    </view>

    <view class="login__card">
      <view class="role-switch">
        <view
          class="role-switch__item"
          :class="{ 'role-switch__item--active': client === 'user' }"
          @tap="client = 'user'"
        >
          <wd-icon name="user" size="34rpx" />
          <text>我是用户</text>
        </view>
        <view
          class="role-switch__item"
          :class="{ 'role-switch__item--active': client === 'boss' }"
          @tap="client = 'boss'"
        >
          <wd-icon name="shop" size="34rpx" />
          <text>我是回收商</text>
        </view>
      </view>

      <view class="login__form">
        <view class="field">
          <wd-icon name="mobile" size="36rpx" color="#9ca3af" />
          <input
            v-model="phone"
            class="field__input"
            type="number"
            :maxlength="11"
            placeholder="请输入手机号"
            placeholder-class="field__placeholder"
          />
        </view>
        <view class="field">
          <wd-icon name="secured" size="36rpx" color="#9ca3af" />
          <input
            v-model="smsCode"
            class="field__input"
            type="number"
            :maxlength="6"
            placeholder="请输入验证码"
            placeholder-class="field__placeholder"
          />
          <view
            class="field__code-btn"
            :class="{ 'field__code-btn--disabled': counting > 0 || sending }"
            @tap="handleSendCode"
          >
            {{ counting > 0 ? `${counting}s 后重发` : "获取验证码" }}
          </view>
        </view>

        <wd-button
          type="primary"
          block
          size="large"
          custom-class="login__submit"
          :loading="logging"
          @click="handleLogin"
        >
          登 录
        </wd-button>
      </view>

      <!-- #ifdef MP-WEIXIN -->
      <view class="third-party">
        <view class="third-party__divider">其他登录方式</view>
        <wd-button plain type="success" icon="chat" @click="onThirdPlaceholder">
          微信一键登录
        </wd-button>
      </view>
      <!-- #endif -->
      <!-- #ifdef MP-ALIPAY -->
      <view class="third-party">
        <view class="third-party__divider">其他登录方式</view>
        <wd-button plain type="primary" icon="wallet" @click="onThirdPlaceholder">
          支付宝一键登录
        </wd-button>
      </view>
      <!-- #endif -->

      <view class="login__tips">
        <view class="login__tips-title">
          <wd-icon name="info-circle" size="28rpx" color="#07c160" />
          <text>联调测试账号（验证码 123456）</text>
        </view>
        <view class="login__tips-row" @tap="fillDemo('user')">
          用户端：13800000001
          <text class="login__tips-fill">一键填入</text>
        </view>
        <view class="login__tips-row" @tap="fillDemo('boss')">
          回收商：13800000002
          <text class="login__tips-fill">一键填入</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { sendSmsCode, phoneLogin } from "@/api/auth";
import { useUserStore } from "@/store/user";

const userStore = useUserStore();

const client = ref<"user" | "boss">("user");
const phone = ref("");
const smsCode = ref("");
const counting = ref(0);
const sending = ref(false);
const logging = ref(false);
let timer: ReturnType<typeof setInterval> | null = null;

function fillDemo(type: "user" | "boss") {
  client.value = type;
  phone.value = type === "user" ? "13800000001" : "13800000002";
  smsCode.value = "123456";
}

function validPhone() {
  if (!/^1\d{10}$/.test(phone.value)) {
    uni.showToast({ title: "请输入正确的手机号", icon: "none" });
    return false;
  }
  return true;
}

async function handleSendCode() {
  if (counting.value > 0 || sending.value) return;
  if (!validPhone()) return;
  sending.value = true;
  try {
    await sendSmsCode(phone.value);
    uni.showToast({ title: "验证码已发送", icon: "none" });
    counting.value = 60;
    timer && clearInterval(timer);
    timer = setInterval(() => {
      counting.value -= 1;
      if (counting.value <= 0 && timer) clearInterval(timer);
    }, 1000);
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    sending.value = false;
  }
}

async function handleLogin() {
  if (!validPhone()) return;
  if (!smsCode.value) {
    uni.showToast({ title: "请输入验证码", icon: "none" });
    return;
  }
  logging.value = true;
  try {
    const data = await phoneLogin({
      phone: phone.value,
      smsCode: smsCode.value,
      client: client.value,
    });
    userStore.setLogin(data);
    uni.reLaunch({ url: userStore.homePath });
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    logging.value = false;
  }
}

function onThirdPlaceholder() {
  uni.showToast({ title: "小程序端接入后开放", icon: "none" });
}
</script>

<style lang="scss" scoped>
.login {
  min-height: 100vh;
  background: $page-bg;

  &__hero {
    background: $theme-gradient;
    padding: 140rpx 48rpx 160rpx;
    border-radius: 0 0 48rpx 48rpx;
  }

  &__logo {
    width: 112rpx;
    height: 112rpx;
    border-radius: 32rpx;
    background: rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__title {
    margin-top: 28rpx;
    font-size: 48rpx;
    font-weight: 700;
    color: #fff;
  }

  &__subtitle {
    margin-top: 12rpx;
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.85);
  }

  &__card {
    margin: -96rpx 32rpx 0;
    background: #fff;
    border-radius: 28rpx;
    padding: 40rpx 36rpx;
    box-shadow: 0 12rpx 40rpx rgba(31, 35, 41, 0.06);
  }

  &__form {
    margin-top: 36rpx;
  }

  :deep(.login__submit) {
    margin-top: 48rpx;
    border-radius: 48rpx !important;
  }

  &__tips {
    margin-top: 44rpx;
    background: $theme-color-light;
    border-radius: 20rpx;
    padding: 24rpx 28rpx;
    font-size: 24rpx;
    color: #4e5969;

    &-title {
      display: flex;
      align-items: center;
      gap: 8rpx;
      font-weight: 600;
      color: #1f2329;
      margin-bottom: 12rpx;
    }

    &-row {
      display: flex;
      justify-content: space-between;
      padding: 8rpx 0;
    }

    &-fill {
      color: $theme-color;
      font-weight: 600;
    }
  }
}

.role-switch {
  display: flex;
  background: #f2f3f5;
  border-radius: 20rpx;
  padding: 8rpx;

  &__item {
    flex: 1;
    height: 84rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10rpx;
    border-radius: 16rpx;
    font-size: 30rpx;
    color: #4e5969;
    transition: all 0.2s;

    &--active {
      background: #fff;
      color: $theme-color;
      font-weight: 600;
      box-shadow: 0 4rpx 12rpx rgba(31, 35, 41, 0.08);
    }
  }
}

.field {
  display: flex;
  align-items: center;
  gap: 20rpx;
  height: 104rpx;
  border-bottom: 1rpx solid #ebedf0;

  &__input {
    flex: 1;
    font-size: 30rpx;
    height: 100%;
  }

  &__placeholder {
    color: #c0c4cc;
  }

  &__code-btn {
    flex-shrink: 0;
    color: $theme-color;
    font-size: 28rpx;
    font-weight: 600;
    padding: 12rpx 0 12rpx 24rpx;

    &--disabled {
      color: #c0c4cc;
    }
  }
}

.third-party {
  margin-top: 40rpx;

  &__divider {
    text-align: center;
    font-size: 24rpx;
    color: #c0c4cc;
    margin-bottom: 24rpx;
  }
}
</style>
