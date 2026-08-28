import { get, post, put } from "@/utils/request";
import type { PageResult } from "@/utils/request";

export interface WalletLedgerItem {
  id: string;
  amount: string;
  bizType?: string;
  bizId?: string;
  remark?: string;
  createTime?: string;
}

export interface WalletData {
  balance: string;
  list: WalletLedgerItem[];
}

export function getWallet() {
  return get<WalletData>("/app-api/user/wallet");
}

/** 钱包余额提现(mock 秒到) */
export function withdrawWallet(amount: string) {
  return post("/app-api/user/wallet/withdraw", { amount }, { loading: true });
}

export interface NoticeItem {
  id: string;
  channel?: string;
  templateKey?: string;
  bizType?: string;
  bizId?: string;
  title?: string;
  content?: string;
  status?: string;
  createTime?: string;
}

export function getUserNotices(params: { pageNum?: number; pageSize?: number }) {
  return get<PageResult<NoticeItem>>("/app-api/user/notices", params, { silent: true });
}

/** 未读消息数(「我的」页角标)，后端返回 { count } */
export async function getUnreadNoticeCount(): Promise<number> {
  const res = await get<{ count?: number } | number>(
    "/app-api/user/notices/unread-count",
    undefined,
    { silent: true }
  );
  return typeof res === "number" ? res : Number(res?.count) || 0;
}

/** 全部标记已读(进入消息页时调用) */
export function readAllNotices() {
  return post("/app-api/user/notices/read-all", {}, { silent: true });
}

export function updateProfile(data: { nickname?: string; avatar?: string }) {
  return put("/app-api/user/profile", data, { loading: true });
}

/** 环保成就统计 */
export interface UserStats {
  /** 完成订单数 */
  completedOrders?: number;
  /** 累计回收重量 kg */
  totalWeightKg?: string | number;
  /** 累计到手金额 */
  totalAmount?: string | number;
  /** 减碳量 kg(每回收 1kg 按 0.8kg 估算) */
  co2SavedKg?: string | number;
}

export function getUserStats() {
  return get<UserStats>("/app-api/user/stats", undefined, { silent: true });
}
