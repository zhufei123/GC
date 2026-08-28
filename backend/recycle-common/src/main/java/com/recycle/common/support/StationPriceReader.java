package com.recycle.common.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.common.entity.store.StationSkuPrice;
import com.recycle.common.mapper.StationSkuPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 门店报价读取：status=1（报价中）的门店 SKU 价格
 */
@Component
@RequiredArgsConstructor
public class StationPriceReader {

    private final StationSkuPriceMapper stationSkuPriceMapper;

    public BigDecimal currentPrice(Long stationId, Long skuId) {
        return currentPrices(stationId, List.of(skuId)).get(skuId);
    }

    /** 指定门店报价中的价格：skuId -> price */
    public Map<Long, BigDecimal> currentPrices(Long stationId, Collection<Long> skuIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (stationId == null || skuIds == null || skuIds.isEmpty()) {
            return result;
        }
        stationSkuPriceMapper.selectList(new LambdaQueryWrapper<StationSkuPrice>()
                        .eq(StationSkuPrice::getStationId, stationId)
                        .in(StationSkuPrice::getSkuId, skuIds)
                        .eq(StationSkuPrice::getStatus, 1))
                .forEach(p -> result.put(p.getSkuId(), p.getPrice()));
        return result;
    }

    /** 指定门店全部报价中的行 */
    public List<StationSkuPrice> quotedRows(Long stationId) {
        if (stationId == null) {
            return List.of();
        }
        return stationSkuPriceMapper.selectList(new LambdaQueryWrapper<StationSkuPrice>()
                .eq(StationSkuPrice::getStationId, stationId)
                .eq(StationSkuPrice::getStatus, 1));
    }

    /** 批量门店报价中的行：stationId -> rows */
    public Map<Long, List<StationSkuPrice>> quotedRowsByStations(Collection<Long> stationIds) {
        if (stationIds == null || stationIds.isEmpty()) {
            return Map.of();
        }
        return stationSkuPriceMapper.selectList(new LambdaQueryWrapper<StationSkuPrice>()
                        .in(StationSkuPrice::getStationId, stationIds)
                        .eq(StationSkuPrice::getStatus, 1))
                .stream()
                .collect(Collectors.groupingBy(StationSkuPrice::getStationId));
    }
}
