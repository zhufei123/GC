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
      <view v-if="bindMode" class="bind-tip">
        <wd-icon name="mobile" size="32rpx" color="#07c160" />
        <text>登录成功，请绑定手机号（已注册号码将自动合并账号）</text>
      </view>

      <!-- 小程序原生手机号快捷绑定 -->
      <view v-if="bindMode" class="quick-bind">
        <!-- #ifdef MP-WEIXIN -->
        <button
          class="quick-bind__btn"
          open-type="getPhoneNumber"
          @getphonenumber="onWxPhoneNumber"
        >
          微信手机号快捷绑定
        </button>
        <!-- #endif -->
        <!-- #ifdef MP-ALIPAY -->
        <button
          class="quick-bind__btn quick-bind__btn--alipay"
          open-type="getAuthorize"
          scope="phoneNumber"
          @getAuthorize="onAlipayPhoneAuth"
          @error="onAlipayPhoneAuthFail"
        >
          支付宝手机号快捷绑定
        </button>
        <!-- #endif -->
        <!-- #ifdef MP-WEIXIN || MP-ALIPAY -->
        <view class="quick-bind__divider">或使用短信验证码绑定</view>
        <!-- #endif -->
      </view>
      <view v-else class="role-switch">
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
          {{ bindMode ? "绑定并登录" : "登 录" }}
        </wd-button>
      </view>

      <view v-if="!bindMode" class="third-party">
        <!-- #ifdef MP-WEIXIN || MP-ALIPAY || H5 -->
        <view class="third-party__divider">其他登录方式</view>
        <!-- #endif -->
        <view class="third-party__btns">
          <!-- #ifdef MP-WEIXIN || H5 -->
          <wd-button plain type="success" icon="chat" :loading="thirdLogging" @click="handleWxLogin">
            微信一键登录
          </wd-button>
          <!-- #endif -->
          <!-- #ifdef MP-ALIPAY || H5 -->
          <wd-button plain type="primary" icon="wallet" :loading="thirdLogging" @click="handleAlipayLogin">
            支付宝一键登录
          </wd-button>
          <!-- #endif -->
        </view>
      </view>

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
import { onLoad } from "@dcloudio/uni-app";
import {
  sendSmsCode,
  phoneLogin,
  wxLogin as apiWxLogin,
  alipayLogin as apiAlipayLogin,
  bindPhone as apiBindPhone,
  bindPhoneWx as apiBindPhoneWx,
  bindPhoneAlipay as apiBindPhoneAlipay,
  type ThirdLoginProfile,
} from "@/api/auth";
import { useUserStore, type LoginPayload } from "@/store/user";

/** 支付宝小程序全局对象(仅 MP-ALIPAY 运行时存在) */
declare const my: any;

const userStore = useUserStore();

const client = ref<"user" | "boss">("user");
const phone = ref("");
const smsCode = ref("");
const counting = ref(0);
const sending = ref(false);
const logging = ref(false);
/** 三方登录成功但未绑手机号：进入补绑模式 */
const bindMode = ref(false);
const thirdLogging = ref(false);
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
  if (bindMode.value) {
    await handleBindPhone();
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

/** 三方登录后补绑手机号（已注册号码后端自动合并账号并返回新 token） */
async function handleBindPhone() {
  logging.value = true;
  try {
    const data = await apiBindPhone({ phone: phone.value, smsCode: smsCode.value });
    userStore.setLogin(data);
    uni.reLaunch({ url: userStore.homePath });
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    logging.value = false;
  }
}

/** 取 uni.login code(支付宝返回 authCode)；失败返回空串由调用方处理 */
function uniLoginCode(provider: "weixin" | "alipay"): Promise<string> {
  return new Promise((resolve) => {
    try {
      uni.login({
        provider,
        success: (res: any) => resolve(res?.code || res?.authCode || ""),
        fail: () => resolve(""),
      } as any);
    } catch (e) {
      resolve("");
    }
  });
}

/**
 * 微信资料授权（getUserProfile 须在点击手势内发起）：
 * 拒绝授权/新版微信返回匿名「微信用户」时返回空对象，不阻断登录。
 */
function wxUserProfile(): Promise<ThirdLoginProfile> {
  return new Promise((resolve) => {
    // #ifdef MP-WEIXIN
    try {
      (uni as any).getUserProfile({
        desc: "用于完善会员资料与订单通知",
        success: (res: any) => {
          const info = res?.userInfo || {};
          const anonymous = !info.nickName || info.nickName === "微信用户";
          resolve({
            nickname: anonymous ? undefined : info.nickName,
            avatar: info.avatarUrl || undefined,
            gender: info.gender > 0 ? info.gender : undefined,
            city: info.city || undefined,
          });
        },
        fail: () => resolve({}),
      });
      return;
    } catch (e) {
      resolve({});
      return;
    }
    // #endif
    resolve({});
  });
}

/** 支付宝会员资料（getOpenUserInfo 需在开放平台开通「获取会员信息」）；未开通/拒绝返回空对象 */
function alipayUserProfile(): Promise<ThirdLoginProfile> {
  return new Promise((resolve) => {
    // #ifdef MP-ALIPAY
    try {
      my.getOpenUserInfo({
        success: (res: any) => {
          try {
            const raw = typeof res?.response === "string" ? JSON.parse(res.response) : res?.response;
            const info = raw?.response || raw || {};
            resolve({
              nickname: info.nickName || undefined,
              avatar: info.avatar || undefined,
              gender: info.gender === "m" ? 1 : info.gender === "f" ? 2 : undefined,
              city: info.city || undefined,
            });
          } catch (e) {
            resolve({});
          }
        },
        fail: () => resolve({}),
      });
      return;
    } catch (e) {
      resolve({});
      return;
    }
    // #endif
    resolve({});
  });
}

function afterThirdLogin(data: LoginPayload) {
  userStore.setLogin(data);
  if (!data.hasPhone) {
    bindMode.value = true;
    uni.showToast({ title: "请先绑定手机号", icon: "none" });
    return;
  }
  uni.reLaunch({ url: userStore.homePath });
}

async function handleWxLogin() {
  if (thirdLogging.value) return;
  thirdLogging.value = true;
  try {
    // 资料授权须在点击手势内先发起，拿到什么传什么（拒绝授权不阻断登录）
    const profile = await wxUserProfile();
    let code = await uniLoginCode("weixin");
    // #ifdef H5
    // H5 无小程序环境：回退 mock code（后端未配置 appid 时 code 即 openid）
    if (!code) code = "h5-mock-wx";
    if (!profile.nickname) {
      profile.nickname = "微信体验用户";
      profile.avatar = "/static/avatar/wx-mock.png";
      profile.gender = 1;
      profile.city = "深圳市";
    }
    // #endif
    if (!code) {
      // 真机小程序环境拿不到 code 说明微信登录异常，不能用 mock 冒充
      uni.showToast({ title: "微信登录失败", icon: "none" });
      return;
    }
    afterThirdLogin(await apiWxLogin(code, client.value, profile));
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    thirdLogging.value = false;
  }
}

async function handleAlipayLogin() {
  if (thirdLogging.value) return;
  thirdLogging.value = true;
  try {
    const profile = await alipayUserProfile();
    let authCode = await uniLoginCode("alipay");
    // #ifdef H5
    if (!authCode) authCode = "h5-mock-alipay";
    if (!profile.nickname) {
      profile.nickname = "支付宝体验用户";
      profile.avatar = "/static/avatar/alipay-mock.png";
      profile.gender = 1;
      profile.city = "杭州市";
    }
    // #endif
    if (!authCode) {
      uni.showToast({ title: "支付宝登录失败", icon: "none" });
      return;
    }
    afterThirdLogin(await apiAlipayLogin(authCode, client.value, profile));
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    thirdLogging.value = false;
  }
}

/** 微信原生手机号快捷绑定：button open-type=getPhoneNumber 回调 */
async function onWxPhoneNumber(e: any) {
  const code = e?.detail?.code;
  if (!code) {
    uni.showToast({ title: "未授权手机号，可用短信绑定", icon: "none" });
    return;
  }
  logging.value = true;
  try {
    const data = await apiBindPhoneWx(code);
    userStore.setLogin(data);
    uni.reLaunch({ url: userStore.homePath });
  } catch (err) {
    /* 错误提示已由 request 统一处理，可继续用短信绑定 */
  } finally {
    logging.value = false;
  }
}

/** 支付宝手机号授权成功后拉加密手机号并提交后端解密绑定；失败提示走短信 */
function onAlipayPhoneAuth() {
  // #ifdef MP-ALIPAY
  try {
    my.getPhoneNumber({
      success: async (res: any) => {
        // response 为加密原文(JSON 字符串)，由后端解密取号
        const encryptedData =
          typeof res?.response === "string" ? res.response : JSON.stringify(res?.response ?? "");
        if (!encryptedData) {
          uni.showToast({ title: "请用短信绑定", icon: "none" });
          return;
        }
        try {
          const data = await apiBindPhoneAlipay(encryptedData);
          userStore.setLogin(data);
          uni.reLaunch({ url: userStore.homePath });
        } catch (err) {
          uni.showToast({ title: "请用短信绑定", icon: "none" });
        }
      },
      fail: () => {
        uni.showToast({ title: "请用短信绑定", icon: "none" });
      },
    });
  } catch (e) {
    uni.showToast({ title: "请用短信绑定", icon: "none" });
  }
  // #endif
}

function onAlipayPhoneAuthFail() {
  uni.showToast({ title: "未授权手机号，可用短信绑定", icon: "none" });
}

onLoad((options) => {
  // 已登录但未绑手机号的场景(如下单被拦截)直接进入补绑模式
  if (options?.bind === "1" && userStore.isLogin) {
    bindMode.value = true;
  }
});
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

  &__btns {
    display: flex;
    flex-direction: column;
    gap: 20rpx;
  }
}

.bind-tip {
  display: flex;
  align-items: center;
  gap: 12rpx;
  background: $theme-color-light;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  font-size: 25rpx;
  color: #1f2329;
}

.quick-bind {
  margin-top: 28rpx;

  &__btn {
    width: 100%;
    height: 88rpx;
    line-height: 88rpx;
    border-radius: 44rpx;
    background: $theme-color;
    color: #fff;
    font-size: 30rpx;
    font-weight: 600;
    border: none;

    &::after {
      border: none;
    }

    &--alipay {
      background: #4d80f0;
    }
  }

  &__divider {
    margin-top: 24rpx;
    text-align: center;
    font-size: 24rpx;
    color: #c0c4cc;
  }
}
</style>
