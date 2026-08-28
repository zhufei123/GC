import { post } from "@/utils/request";

export type UploadScene = "weigh" | "order" | "avatar";

/** COS 上传签名返回(骨架期后端固定 mock=true，契约见 docs/06) */
interface CosSignVO {
  bucket?: string;
  region?: string;
  keys?: string[];
  cdnHost?: string;
  mock?: boolean;
}

/**
 * 选图并上传，返回可直接展示的图片地址列表。
 * 骨架期签名接口返回 mock credentials，不做真实 COS 直传：
 * - H5:chooseImage 的 blob 临时路径刷新即失效，压缩后转 base64 data URL，
 *   随订单提交后管理端 el-image / 小程序 image 均可直接展示
 * - 小程序:保留 tempFilePath 供本端预览提交
 * 正式 STS 就绪(mock=false)后在此改为直传并返回 cdnHost + key。
 */
export async function chooseAndUpload(scene: UploadScene, count = 6): Promise<string[]> {
  const paths = await chooseImages(count);
  if (!paths.length) return [];
  try {
    await post<CosSignVO>(
      "/app-api/common/cos/upload-sign",
      { scene, ext: "jpg", count: paths.length },
      { silent: true }
    );
  } catch (e) {
    /* 签名失败也按 mock 降级处理，不阻断业务流程 */
  }
  return Promise.all(paths.map(toDisplayableUrl));
}

function chooseImages(count: number): Promise<string[]> {
  return new Promise((resolve) => {
    uni.chooseImage({
      count,
      sizeType: ["compressed"],
      sourceType: ["album", "camera"],
      success: (res) => resolve(((res.tempFilePaths as string[]) || []).slice(0, count)),
      fail: () => resolve([]),
    });
  });
}

async function toDisplayableUrl(path: string): Promise<string> {
  // #ifdef H5
  try {
    return await compressToDataUrl(path);
  } catch (e) {
    return path;
  }
  // #endif
  // #ifndef H5
  return path;
  // #endif
}

// #ifdef H5
/** 长边压到 1000px、jpeg 0.7，控制 base64 体积(单张约 100-300KB) */
const MAX_EDGE = 1000;

function compressToDataUrl(blobUrl: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => {
      const scale = Math.min(1, MAX_EDGE / Math.max(img.width, img.height));
      const canvas = document.createElement("canvas");
      canvas.width = Math.max(1, Math.round(img.width * scale));
      canvas.height = Math.max(1, Math.round(img.height * scale));
      const ctx = canvas.getContext("2d");
      if (!ctx) {
        reject(new Error("canvas unavailable"));
        return;
      }
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      resolve(canvas.toDataURL("image/jpeg", 0.7));
    };
    img.onerror = () => reject(new Error("load image failed"));
    img.src = blobUrl;
  });
}
// #endif
