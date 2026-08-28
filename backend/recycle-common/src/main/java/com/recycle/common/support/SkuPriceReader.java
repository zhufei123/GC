package com.recycle.common.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.common.entity.recycle.SkuPrice;
import com.recycle.common.mapper.SkuPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 当前生效价读取：effective_at <= now 的最新一条（status=1）
 */
@Component
@RequiredArgsConstructor
public class SkuPriceReader {

    private final SkuPriceMapper skuPriceMapper;

    public BigDecimal currentPrice(Long skuId) {
        return currentPrices(List.of(skuId)).get(skuId);
    }

    public Map<Long, BigDecimal> currentPrices(Collection<Long> skuIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (skuIds == null || skuIds.isEmpty()) {
            return result;
        }
        List<SkuPrice> prices = skuPriceMapper.selectList(new LambdaQueryWrapper<SkuPrice>()
                .in(SkuPrice::getSkuId, skuIds)
                .eq(SkuPrice::getStatus, 1)
                .le(SkuPrice::getEffectiveAt, LocalDateTime.now()));
        prices.stream()
                .sorted(Comparator.comparing(SkuPrice::getEffectiveAt)
                        .thenComparing(SkuPrice::getId))
                .forEach(p -> result.put(p.getSkuId(), p.getPrice()));
        return result;
    }
}
