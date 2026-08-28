import { defineStore } from "pinia";

export type Role = "customer" | "recycler";
export type RecyclerStatus = "none" | "pending" | "approved" | "rejected";

export interface LoginPayload {
  token: string;
  userId: string;
  role: Role;
  recyclerStatus: RecyclerStatus;
  nickname: string;
  storeId?: string | null;
  /** 三方登录返回：false 时需补绑手机号 */
  hasPhone?: boolean;
  isNewUser?: boolean;
}

const STORAGE_KEY = "RECYCLE_APP_USER";

export const useUserStore = defineStore("user", {
  state: () => ({
    token: "",
    userId: "",
    role: "customer" as Role,
    recyclerStatus: "none" as RecyclerStatus,
    nickname: "",
    storeId: "" as string | null,
  }),
  getters: {
    isLogin(state): boolean {
      return !!state.token;
    },
    isApprovedRecycler(state): boolean {
      return state.role === "recycler" && state.recyclerStatus === "approved";
    },
    homePath(): string {
      return this.isApprovedRecycler ? "/pages-recycler/index" : "/pages-customer/index";
    },
  },
  actions: {
    setLogin(data: LoginPayload) {
      this.token = data.token || "";
      this.userId = String(data.userId ?? "");
      this.role = (data.role as Role) || "customer";
      this.recyclerStatus = (data.recyclerStatus as RecyclerStatus) || "none";
      this.nickname = data.nickname || "";
      this.storeId = data.storeId ? String(data.storeId) : "";
      this.persist();
    },
    setRecyclerStatus(status: RecyclerStatus) {
      this.recyclerStatus = status;
      this.persist();
    },
    persist() {
      uni.setStorageSync(
        STORAGE_KEY,
        JSON.stringify({
          token: this.token,
          userId: this.userId,
          role: this.role,
          recyclerStatus: this.recyclerStatus,
          nickname: this.nickname,
          storeId: this.storeId,
        })
      );
    },
    restore() {
      try {
        const raw = uni.getStorageSync(STORAGE_KEY);
        if (raw) {
          const data = JSON.parse(raw);
          this.token = data.token || "";
          this.userId = data.userId || "";
          this.role = data.role || "customer";
          this.recyclerStatus = data.recyclerStatus || "none";
          this.nickname = data.nickname || "";
          this.storeId = data.storeId || "";
        }
      } catch (e) {
        /* 忽略脏数据 */
      }
    },
    logout() {
      this.token = "";
      this.userId = "";
      this.role = "customer";
      this.recyclerStatus = "none";
      this.nickname = "";
      this.storeId = "";
      uni.removeStorageSync(STORAGE_KEY);
    },
  },
});
