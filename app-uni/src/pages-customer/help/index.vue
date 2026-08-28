<template>
  <view class="help">
    <view class="help__banner">
      <wd-icon name="help-circle-filled" size="64rpx" color="#ffffff" />
      <view>
        <view class="help__banner-title">帮助中心</view>
        <view class="help__banner-sub">常见问题一站解答</view>
      </view>
    </view>

    <view class="help__card">
      <wd-collapse v-model="opened">
        <wd-collapse-item
          v-for="faq in FAQS"
          :key="faq.name"
          :name="faq.name"
          :title="faq.title"
        >
          <view class="help__answer">
            <view v-for="(line, i) in faq.lines" :key="i" class="help__line">{{ line }}</view>
          </view>
        </wd-collapse-item>
      </wd-collapse>
    </view>

    <view class="help__contact">
      如仍有疑问，可在订单页联系对应回收站，或通过「我的 - 关于绿色回收」了解更多。
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";

const FAQS = [
  {
    name: "order",
    title: "怎么下单？",
    lines: [
      "1. 在首页点击「预约上门回收」，或进入回收站详情选择「上门回收 / 自行送货」。",
      "2. 选择回收品类并填写预估重量，系统按门店报价自动估算金额。",
      "3. 上门单需选择收货地址和期望上门时间；到店单直接选择门店即可。",
      "4. 提交后等待回收站接单，可在「订单」页查看进度。",
    ],
  },
  {
    name: "station",
    title: "怎么选回收站？",
    lines: [
      "1. 首页点击「附近回收站」，按距离或报价排序挑选。",
      "2. 在「价格」页点击任意品类，可对比附近各站对该品类的报价。",
      "3. 进入门店详情可查看营业时间、完整价目、用户评分，并可收藏常用站点。",
      "4. 收藏后在「我的 - 我的收藏」快速找回。",
    ],
  },
  {
    name: "settle",
    title: "怎么结算？",
    lines: [
      "1. 回收员上门（或您到店）后现场称重，按称重明细生成实收金额。",
      "2. 称重完成后订单进入「已称重」状态，金额锁定，双方确认无误后完成订单。",
      "3. 结算以称重时门店发布的报价为准，明细可在订单详情逐项核对。",
    ],
  },
  {
    name: "payment",
    title: "线上打款说明",
    lines: [
      "1. 当前版本货款由回收站与您当面现金 / 转账结清，平台暂不代收代付。",
      "2. 订单完成前请务必确认已收到货款，再让回收员完成订单。",
      "3. 线上钱包与自动打款功能正在建设中，上线后将支持货款直接到账。",
    ],
  },
  {
    name: "privacy",
    title: "隐私与信息安全",
    lines: [
      "1. 您的手机号、地址仅用于回收履约，仅对已接单的回收站可见。",
      "2. 接单大厅中展示的联系人做了脱敏处理，不会泄露完整姓名。",
      "3. 订单照片仅用于核对品类与重量，不作他用。",
      "4. 如需删除账号数据，可通过「我的 - 关于绿色回收」联系我们处理。",
    ],
  },
];

const opened = ref<string[]>(["order"]);
</script>

<style lang="scss" scoped>
.help {
  min-height: 100vh;
  background: $page-bg;
  padding: 24rpx 32rpx 48rpx;
  box-sizing: border-box;

  &__banner {
    display: flex;
    align-items: center;
    gap: 24rpx;
    background: $theme-gradient;
    border-radius: 24rpx;
    padding: 40rpx 32rpx;
    margin-bottom: 24rpx;
  }

  &__banner-title {
    color: #fff;
    font-size: 36rpx;
    font-weight: 700;
  }

  &__banner-sub {
    margin-top: 8rpx;
    color: rgba(255, 255, 255, 0.85);
    font-size: 24rpx;
  }

  &__card {
    background: #fff;
    border-radius: 24rpx;
    overflow: hidden;
  }

  &__answer {
    padding: 4rpx 0;
  }

  &__line {
    font-size: 26rpx;
    color: #4e5969;
    line-height: 1.7;

    & + & {
      margin-top: 8rpx;
    }
  }

  &__contact {
    margin-top: 24rpx;
    text-align: center;
    font-size: 22rpx;
    color: #c0c4cc;
    padding: 0 24rpx;
  }
}
</style>
