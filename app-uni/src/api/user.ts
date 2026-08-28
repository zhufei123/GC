import { get, put } from "@/utils/request";
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

export function updateProfile(data: { nickname?: string; avatar?: string }) {
  return put("/app-api/user/profile", data, { loading: true });
}
