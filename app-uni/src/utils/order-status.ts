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
