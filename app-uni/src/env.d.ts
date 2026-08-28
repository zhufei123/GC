/// <reference types="vite/client" />

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  // eslint-disable-next-line @typescript-eslint/no-explicit-any, @typescript-eslint/ban-types
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  /** 地图提供商: amap | qqmap | osm(默认 amap,key 缺失时降级) */
  readonly VITE_MAP_PROVIDER: string
  readonly VITE_AMAP_KEY: string
  readonly VITE_AMAP_SECURITY_CODE: string
  readonly VITE_MAP_QQ_KEY: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
