import { get } from "@/utils/request";

export interface BannerItem {
  id?: string;
  title?: string;
  image?: string;
  imageUrl?: string;
  link?: string;
}

export interface HomeData {
  banners?: BannerItem[];
  hotCategories?: Array<{ id: string; name: string; icon?: string }>;
  notices?: Array<{ id: string; title: string }>;
}

export function getHome() {
  return get<HomeData>("/app-api/home", undefined, { silent: true });
}
