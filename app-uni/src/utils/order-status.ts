export const ORDER_STATUS_TEXT: Record<string, string> = {
  PENDING: "待接单",
  ACCEPTED: "已接单",
  SERVING: "服务中",
  WEIGHED: "已称重",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
};

/** wd-tag 的 type */
export const ORDER_STATUS_TYPE: Record<string, string> = {
  PENDING: "warning",
  ACCEPTED: "primary",
  SERVING: "primary",
  WEIGHED: "success",
  COMPLETED: "success",
  CANCELLED: "default",
};

export function statusText(status?: string) {
  return (status && ORDER_STATUS_TEXT[status]) || status || "-";
}

export function statusType(status?: string) {
  return (status && ORDER_STATUS_TYPE[status]) || "default";
}

export const PAY_METHOD_TEXT: Record<string, string> = {
  OFFLINE: "线下现金",
  WX_TRANSFER: "微信打款",
  ALIPAY_TRANSFER: "支付宝打款",
  WALLET: "平台钱包",
};

export const PAYOUT_STATUS_TEXT: Record<string, string> = {
  SUCCESS: "打款成功",
  PROCESSING: "打款处理中",
  WAIT_USER_CONFIRM: "待用户确认收款",
  FAILED: "打款失败",
};

export function payMethodText(payMethod?: string) {
  return (payMethod && PAY_METHOD_TEXT[payMethod]) || payMethod || "-";
}

export function payoutStatusText(payoutStatus?: string) {
  return (payoutStatus && PAYOUT_STATUS_TEXT[payoutStatus]) || payoutStatus || "-";
}
