package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.app.vo.CategoryNodeVO;
import com.recycle.app.vo.SkuVO;
import com.recycle.common.entity.recycle.Category;
import com.recycle.common.entity.recycle.Sku;
import com.recycle.common.entity.recycle.SkuPriceLog;
import com.recycle.common.mapper.CategoryMapper;
import com.recycle.common.mapper.SkuMapper;
import com.recycle.common.mapper.SkuPriceLogMapper;
import com.recycle.common.support.SkuPriceReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppRecycleService {

    private final CategoryMapper categoryMapper;
    private final SkuMapper skuMapper;
    private final SkuPriceLogMapper skuPriceLogMapper;
    private final SkuPriceReader skuPriceReader;

    /** 仅 status=1 的分类树 */
    public List<CategoryNodeVO> categoryTree() {
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort));
        Map<Long, CategoryNodeVO> nodes = new HashMap<>();
        for (Category c : all) {
            CategoryNodeVO node = new CategoryNodeVO();
            node.setId(c.getId());
            node.setParentId(c.getParentId());
            node.setName(c.getName());
            node.setIcon(c.getIcon());
            node.setSort(c.getSort());
            node.setStatus(c.getStatus());
            nodes.put(c.getId(), node);
        }
        List<CategoryNodeVO> roots = new ArrayList<>();
        for (CategoryNodeVO node : nodes.values()) {
            CategoryNodeVO parent = nodes.get(node.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(node);
            }
        }
        roots.forEach(r -> r.getChildren().sort(Comparator.comparing(CategoryNodeVO::getSort)));
        roots.sort(Comparator.comparing(CategoryNodeVO::getSort));
        return roots;
    }

    /** 上架 SKU + 平台指导价（null=暂无指导价；成交价以门店报价为准） */
    public List<SkuVO> skuList(Long categoryId, String keyword) {
        LambdaQueryWrapper<Sku> wrapper = new LambdaQueryWrapper<Sku>()
                .eq(Sku::getStatus, 1)
                .orderByAsc(Sku::getSort);
        if (categoryId != null) {
            List<Long> categoryIds = new ArrayList<>();
            categoryIds.add(categoryId);
            categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                            .eq(Category::getParentId, categoryId)
                            .eq(Category::getStatus, 1))
                    .forEach(c -> categoryIds.add(c.getId()));
            wrapper.in(Sku::getCategoryId, categoryIds);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Sku::getName, keyword);
        }
        List<Sku> skus = skuMapper.selectList(wrapper);
        Map<Long, BigDecimal> prices =
                skuPriceReader.currentPrices(skus.stream().map(Sku::getId).toList());
        Map<Long, SkuPriceLog> latestLogs = latestPriceLogs(prices.keySet().stream().toList());
        return skus.stream().map(s -> {
            SkuVO vo = new SkuVO();
            vo.setId(s.getId());
            vo.setCategoryId(s.getCategoryId());
            vo.setName(s.getName());
            vo.setImage(s.getImage());
            vo.setUnit(s.getUnit());
            vo.setDescription(s.getDescription());
            vo.setSort(s.getSort());
            vo.setPrice(prices.get(s.getId()));
            fillTrend(vo, latestLogs.get(s.getId()));
            return vo;
        }).toList();
    }

    /** 最新一条调价记录（按生效时间，其次 id） */
    private Map<Long, SkuPriceLog> latestPriceLogs(List<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        return skuPriceLogMapper.selectList(new LambdaQueryWrapper<SkuPriceLog>()
                        .in(SkuPriceLog::getSkuId, skuIds))
                .stream()
                .collect(Collectors.toMap(SkuPriceLog::getSkuId, Function.identity(),
                        (a, b) -> pickLater(a, b)));
    }

    private SkuPriceLog pickLater(SkuPriceLog a, SkuPriceLog b) {
        if (a.getEffectiveAt() != null && b.getEffectiveAt() != null
                && !a.getEffectiveAt().equals(b.getEffectiveAt())) {
            return a.getEffectiveAt().isAfter(b.getEffectiveAt()) ? a : b;
        }
        return a.getId() >= b.getId() ? a : b;
    }

    /** 有报价且有调价记录时给出趋势：newPrice 相对 oldPrice */
    private void fillTrend(SkuVO vo, SkuPriceLog log) {
        if (vo.getPrice() == null || log == null || log.getNewPrice() == null || log.getOldPrice() == null) {
            return;
        }
        BigDecimal diff = log.getNewPrice().subtract(log.getOldPrice());
        vo.setPriceDiff(diff);
        int sign = diff.compareTo(BigDecimal.ZERO);
        vo.setTrend(sign > 0 ? "UP" : sign < 0 ? "DOWN" : "FLAT");
    }
}
