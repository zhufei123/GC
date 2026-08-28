import { get } from "@/utils/request";

export interface CategoryNode {
  id: string;
  parentId?: string;
  name: string;
  icon?: string;
  children?: CategoryNode[];
}

export interface SkuItem {
  id: string;
  categoryId?: string;
  name: string;
  image?: string;
  unit?: string;
  description?: string;
  price?: string | null;
  /** 较昨日涨跌: UP | DOWN | FLAT，无昨日价为空 */
  trend?: string;
  /** 涨跌差价(绝对值字符串) */
  priceDiff?: string;
}

export function getCategoryTree() {
  return get<CategoryNode[]>("/app-api/recycle/category/tree", undefined, { silent: true });
}

export function getSkuList(categoryId?: string) {
  return get<SkuItem[]>("/app-api/recycle/sku/list", categoryId ? { categoryId } : undefined, {
    silent: true,
  });
}

export function searchSkus(keyword: string) {
  return get<SkuItem[]>("/app-api/recycle/sku/search", { keyword }, { silent: true });
}

/**
 * 拉取某分类节点下全部 SKU。
 * 先按节点 id 查询；若后端实现不含子级导致为空，则并发查询叶子子分类合并。
 */
export async function getSkusUnderCategory(node: CategoryNode): Promise<SkuItem[]> {
  try {
    const direct = await getSkuList(node.id);
    if (direct && direct.length) return direct;
  } catch (e) {
    /* 降级到子分类查询 */
  }
  const children = node.children || [];
  if (!children.length) return [];
  const results = await Promise.all(
    children.map((c) => getSkuList(c.id).catch(() => [] as SkuItem[]))
  );
  return results.reduce((acc, cur) => acc.concat(cur || []), [] as SkuItem[]);
}

/** 拉取全部 SKU(下单页用)，同样带分类兜底 */
export async function getAllSkus(): Promise<{ tree: CategoryNode[]; skus: SkuItem[] }> {
  let tree: CategoryNode[] = [];
  try {
    tree = (await getCategoryTree()) || [];
  } catch (e) {
    tree = [];
  }
  try {
    const all = await getSkuList();
    if (all && all.length) return { tree, skus: all };
  } catch (e) {
    /* 降级 */
  }
  const results = await Promise.all(
    tree.map((n) => getSkusUnderCategory(n).catch(() => [] as SkuItem[]))
  );
  return { tree, skus: results.reduce((acc, cur) => acc.concat(cur), [] as SkuItem[]) };
}
