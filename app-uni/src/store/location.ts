import { defineStore } from "pinia";

const STORAGE_KEY = "RECYCLE_APP_LOCATION";

/**
 * 用户选择的城市与坐标（首页城市选择器写入），
 * resolveUserLocation 优先使用这里的坐标。
 */
export const useLocationStore = defineStore("location", {
  state: () => ({
    city: "",
    longitude: 0,
    latitude: 0,
  }),
  getters: {
    /** 已选城市的有效坐标；未选或坐标缺失时为 null */
    coords(state): { longitude: number; latitude: number } | null {
      if (state.longitude && state.latitude) {
        return { longitude: state.longitude, latitude: state.latitude };
      }
      return null;
    },
  },
  actions: {
    setCity(city: string, longitude?: number | null, latitude?: number | null) {
      this.city = city || "";
      this.longitude = Number(longitude) || 0;
      this.latitude = Number(latitude) || 0;
      this.persist();
    },
    persist() {
      uni.setStorageSync(
        STORAGE_KEY,
        JSON.stringify({
          city: this.city,
          longitude: this.longitude,
          latitude: this.latitude,
        })
      );
    },
    restore() {
      try {
        const raw = uni.getStorageSync(STORAGE_KEY);
        if (raw) {
          const data = JSON.parse(raw);
          this.city = data.city || "";
          this.longitude = Number(data.longitude) || 0;
          this.latitude = Number(data.latitude) || 0;
        }
      } catch (e) {
        /* 忽略脏数据 */
      }
    },
  },
});
