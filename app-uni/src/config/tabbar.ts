export interface TabItem {
  title: string;
  icon: string;
}

export const CUSTOMER_TABS: TabItem[] = [
  { title: "首页", icon: "home" },
  { title: "查价", icon: "money-circle" },
  { title: "订单", icon: "list" },
  { title: "我的", icon: "user" },
];

export const RECYCLER_TABS: TabItem[] = [
  { title: "工作台", icon: "dashboard" },
  { title: "接单大厅", icon: "notification" },
  { title: "订单", icon: "list" },
  { title: "我的", icon: "shop" },
];
