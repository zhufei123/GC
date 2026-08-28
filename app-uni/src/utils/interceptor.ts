import { useUserStore } from "@/store/user";

const WHITE_LIST = ["/pages/launch/index", "/pages/login/index"];
const ROUTE_APIS = ["navigateTo", "redirectTo", "reLaunch", "switchTab"] as const;

/**
 * 返回 null 表示放行；否则返回应重定向的路径。
 * 规则:
 * 1. 未登录一律去登录页(白名单除外)
 * 2. 进入 B 端(pages-recycler)非 apply 页时:
 *    - 未提交过申请(recyclerStatus=none) → 入驻申请页
 *    - 申请中/被驳回 → 审核状态页
 *    - 已通过(approved) 放行
 */
export function resolveGuard(url: string): string | null {
  const path = ("/" + url.replace(/^\/+/, "")).split("?")[0];
  if (WHITE_LIST.includes(path)) return null;

  const store = useUserStore();
  if (!store.token) return "/pages/login/index";

  if (path.startsWith("/pages-recycler/") || path === "/pages-recycler/index") {
    if (path.startsWith("/pages-recycler/apply/")) return null;
    if (store.isApprovedRecycler) return null;
    if (store.recyclerStatus === "none") return "/pages-recycler/apply/index";
    return "/pages-recycler/apply/result";
  }
  return null;
}

export function setupInterceptors() {
  ROUTE_APIS.forEach((api) => {
    uni.addInterceptor(api, {
      invoke(args: { url: string }) {
        if (!args || !args.url) return true;
        const to = resolveGuard(args.url);
        if (to) {
          uni.reLaunch({ url: to });
          return false;
        }
        return true;
      },
    });
  });
}
