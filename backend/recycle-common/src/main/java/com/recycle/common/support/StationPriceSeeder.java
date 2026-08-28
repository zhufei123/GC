package com.recycle.common.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.common.entity.recycle.Sku;
import com.recycle.common.entity.store.StationSkuPrice;
import com.recycle.common.mapper.SkuMapper;
import com.recycle.common.mapper.StationSkuPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 新站入驻后以平台指导价预填本站报价，便于立刻被用户看见；已有报价则跳过。
 */
@Component
@RequiredArgsConstructor
public class StationPriceSeeder {

    private final StationSkuPriceMapper stationSkuPriceMapper;
    private final SkuMapper skuMapper;
    private final SkuPriceReader skuPriceReader;

    public int seedFromGuideIfEmpty(Long stationId) {
        if (stationId == null) {
            return 0;
        }
        Long existing = stationSkuPriceMapper.selectCount(new LambdaQueryWrapper<StationSkuPrice>()
                .eq(StationSkuPrice::getStationId, stationId));
        if (existing != null && existing > 0) {
            return 0;
        }
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getStatus, 1));
        if (skus.isEmpty()) {
            return 0;
        }
        Map<Long, BigDecimal> guides = skuPriceReader.currentPrices(skus.stream().map(Sku::getId).toList());
        int n = 0;
        for (Sku sku : skus) {
            BigDecimal price = guides.get(sku.getId());
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            StationSkuPrice row = new StationSkuPrice();
            row.setStationId(stationId);
            row.setSkuId(sku.getId());
            row.setPrice(price);
            row.setStatus(1);
            row.setRemark("入驻同步指导价");
            stationSkuPriceMapper.insert(row);
            n++;
        }
        return n;
    }
}
