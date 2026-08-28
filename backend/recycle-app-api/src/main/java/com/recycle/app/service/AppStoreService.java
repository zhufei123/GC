package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.app.vo.SkuQuoteVO;
import com.recycle.app.vo.StoreDetailVO;
import com.recycle.app.vo.StoreNearbyVO;
import com.recycle.app.vo.StorePriceVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.entity.recycle.Category;
import com.recycle.common.entity.recycle.Sku;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.entity.store.StationSkuPrice;
import com.recycle.common.mapper.CategoryMapper;
import com.recycle.common.mapper.OrderReviewMapper;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.mapper.SkuMapper;
import com.recycle.common.mapper.StationSkuPriceMapper;
import com.recycle.common.support.SkuPriceReader;
import com.recycle.common.support.StationPriceReader;
import com.recycle.common.util.GeoUtils;
import com.recycle.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * C 端门店：附近列表（含报价亮点）、门店详情、门店报价单、单品附近报价
 */
@Service
@RequiredArgsConstructor
public class AppStoreService {

    private static final BigDecimal DEFAULT_RADIUS_KM = BigDecimal.valueOf(20);
    private static final BigDecimal MAX_RADIUS_KM = BigDecimal.valueOf(50);
    private static final int TOP_PRICES = 3;

    private final RecycleStationMapper stationMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final StationSkuPriceMapper stationSkuPriceMapper;
    private final OrderReviewMapper orderReviewMapper;
    private final StationPriceReader stationPriceReader;
    private final SkuPriceReader skuPriceReader;

    public List<StoreNearbyVO> nearby(BigDecimal longitude, BigDecimal latitude,
                                      BigDecimal radiusKm, String sort) {
        BigDecimal radius = clampRadius(radiusKm);
        List<RecycleStation> stations = visibleStations();
        if (stations.isEmpty()) {
            return List.of();
        }
        Map<Long, List<StationSkuPrice>> quotesByStation =
                stationPriceReader.quotedRowsByStations(stations.stream().map(RecycleStation::getId).toList());
        Map<Long, Sku> skus = enabledSkus(quotesByStation.values().stream()
                .flatMap(List::stream).map(StationSkuPrice::getSkuId).distinct().toList());
        Map<Long, Long> skuRoot = skuRootCategory(skus.values());
        List<Long> highlightRoots = highlightRootCategories();

        return stations.stream()
                .map(s -> {
                    StoreNearbyVO vo = new StoreNearbyVO();
                    vo.setId(s.getId());
                    vo.setName(s.getName());
                    vo.setAddress(s.getAddress());
                    vo.setPhone(s.getPhone());
                    vo.setBusinessHours(s.getBusinessHours());
                    vo.setBusinessStatus(s.getBusinessStatus());
                    vo.setLongitude(s.getLongitude());
                    vo.setLatitude(s.getLatitude());
                    vo.setCategoryIds(JsonUtils.toLongList(s.getCategoryIds()));
                    vo.setPhotos(JsonUtils.toStringList(s.getPhotos()));
                    vo.setDistanceKm(GeoUtils.distanceKm(longitude, latitude, s.getLongitude(), s.getLatitude()));
                    vo.setOpenNow(openNow(s));

                    List<StationSkuPrice> quotes = quotesByStation.getOrDefault(s.getId(), List.of()).stream()
                            .filter(q -> skus.containsKey(q.getSkuId()))
                            .sorted(Comparator.comparing(StationSkuPrice::getPrice).reversed())
                            .toList();
                    vo.setQuotedCount(quotes.size());
                    vo.setHighlightPrice(highlightPrice(quotes, skuRoot, highlightRoots));
                    vo.setPrices(quotes.stream().limit(TOP_PRICES).map(q -> {
                        StoreNearbyVO.PriceBriefVO brief = new StoreNearbyVO.PriceBriefVO();
                        brief.setSkuName(skus.get(q.getSkuId()).getName());
                        brief.setPrice(q.getPrice());
                        brief.setUpdatedAt(q.getUpdateTime());
                        return brief;
                    }).toList());
                    return vo;
                })
                .filter(vo -> vo.getDistanceKm() == null || vo.getDistanceKm().compareTo(radius) <= 0)
                .sorted(nearbyComparator(sort))
                .toList();
    }

    public StoreDetailVO detail(Long id, BigDecimal longitude, BigDecimal latitude) {
        RecycleStation s = requireVisible(id);
        StoreDetailVO vo = new StoreDetailVO();
        vo.setId(s.getId());
        vo.setName(s.getName());
        vo.setPhone(s.getPhone());
        vo.setContactName(s.getContactName());
        vo.setProvince(s.getProvince());
        vo.setCity(s.getCity());
        vo.setDistrict(s.getDistrict());
        vo.setAddress(s.getAddress());
        vo.setLongitude(s.getLongitude());
        vo.setLatitude(s.getLatitude());
        vo.setBusinessHours(s.getBusinessHours());
        vo.setBusinessStatus(s.getBusinessStatus());
        vo.setOpenNow(openNow(s));
        vo.setCategoryIds(JsonUtils.toLongList(s.getCategoryIds()));
        vo.setPhotos(JsonUtils.toStringList(s.getPhotos()));
        vo.setDistanceKm(GeoUtils.distanceKm(longitude, latitude, s.getLongitude(), s.getLatitude()));
        List<StationSkuPrice> quoted = stationPriceReader.quotedRows(s.getId());
        Map<Long, Sku> quotedSkus = enabledSkus(quoted.stream()
                .map(StationSkuPrice::getSkuId).distinct().toList());
        vo.setQuotedCount((int) quoted.stream().filter(q -> quotedSkus.containsKey(q.getSkuId())).count());
        fillReviewStats(vo, s.getId());
        return vo;
    }

    /** 单条聚合 SQL 填充评分统计，失败不影响详情主流程 */
    private void fillReviewStats(StoreDetailVO vo, Long stationId) {
        try {
            Map<String, Object> stats = orderReviewMapper.statsByStation(stationId);
            if (stats == null) {
                return;
            }
            Object count = stats.get("reviewCount");
            Object avg = stats.get("avgRating");
            long reviewCount = count instanceof Number n ? n.longValue() : 0L;
            vo.setReviewCount(reviewCount);
            if (reviewCount > 0 && avg instanceof Number n) {
                vo.setAvgRating(BigDecimal.valueOf(n.doubleValue()).setScale(1, RoundingMode.HALF_UP));
            }
        } catch (Exception e) {
            // order_review 表未建时静默降级
        }
    }

    /** 门店报价单（含停报行），附平台指导价对比 */
    public List<StorePriceVO> prices(Long id) {
        RecycleStation s = requireVisible(id);
        List<StationSkuPrice> rows = stationSkuPriceMapper.selectList(
                new LambdaQueryWrapper<StationSkuPrice>()
                        .eq(StationSkuPrice::getStationId, s.getId()));
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Sku> skus = enabledSkus(rows.stream().map(StationSkuPrice::getSkuId).distinct().toList());
        Map<Long, String> categoryNames = categoryMapper.selectByIds(
                        skus.values().stream().map(Sku::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(Category::getId, Category::getName));
        Map<Long, BigDecimal> guidePrices = skuPriceReader.currentPrices(skus.keySet());

        return rows.stream()
                .filter(r -> skus.containsKey(r.getSkuId()))
                .sorted(Comparator.comparing(StationSkuPrice::getStatus, Comparator.reverseOrder())
                        .thenComparing(StationSkuPrice::getPrice, Comparator.reverseOrder()))
                .map(r -> {
                    Sku sku = skus.get(r.getSkuId());
                    StorePriceVO vo = new StorePriceVO();
                    vo.setSkuId(sku.getId());
                    vo.setSkuName(sku.getName());
                    vo.setUnit(sku.getUnit());
                    vo.setCategoryName(categoryNames.get(sku.getCategoryId()));
                    vo.setPrice(r.getPrice());
                    vo.setStatus(r.getStatus());
                    vo.setGuidePrice(guidePrices.get(sku.getId()));
                    vo.setUpdatedAt(r.getUpdateTime());
                    return vo;
                })
                .toList();
    }

    /** 某 SKU 的附近门店报价：距离优先，再价高者优先 */
    public List<SkuQuoteVO> skuQuotes(Long skuId, BigDecimal longitude, BigDecimal latitude, BigDecimal radiusKm) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BizException(ErrorCode.SKU_OFFLINE);
        }
        List<StationSkuPrice> quotes = stationSkuPriceMapper.selectList(
                new LambdaQueryWrapper<StationSkuPrice>()
                        .eq(StationSkuPrice::getSkuId, skuId)
                        .eq(StationSkuPrice::getStatus, 1));
        if (quotes.isEmpty()) {
            return List.of();
        }
        Map<Long, StationSkuPrice> quoteByStation = quotes.stream()
                .collect(Collectors.toMap(StationSkuPrice::getStationId, Function.identity(), (a, b) -> a));
        List<RecycleStation> stations = stationMapper.selectList(new LambdaQueryWrapper<RecycleStation>()
                .in(RecycleStation::getId, quoteByStation.keySet())
                .eq(RecycleStation::getAuditStatus, "approved")
                .eq(RecycleStation::getStatus, 1));
        return stations.stream()
                .map(s -> {
                    StationSkuPrice quote = quoteByStation.get(s.getId());
                    SkuQuoteVO vo = new SkuQuoteVO();
                    vo.setStationId(s.getId());
                    vo.setStationName(s.getName());
                    vo.setAddress(s.getAddress());
                    vo.setLongitude(s.getLongitude());
                    vo.setLatitude(s.getLatitude());
                    vo.setPrice(quote.getPrice());
                    vo.setUpdatedAt(quote.getUpdateTime());
                    vo.setUnit(sku.getUnit());
                    vo.setDistanceKm(GeoUtils.distanceKm(longitude, latitude, s.getLongitude(), s.getLatitude()));
                    vo.setBusinessHours(s.getBusinessHours());
                    vo.setBusinessStatus(s.getBusinessStatus());
                    vo.setOpenNow(openNow(s));
                    return vo;
                })
                .filter(vo -> vo.getDistanceKm() == null || vo.getDistanceKm().compareTo(clampRadius(radiusKm)) <= 0)
                .sorted(Comparator.comparing(SkuQuoteVO::getDistanceKm,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SkuQuoteVO::getPrice, Comparator.reverseOrder()))
                .toList();
    }

    private BigDecimal clampRadius(BigDecimal radiusKm) {
        if (radiusKm == null) {
            return DEFAULT_RADIUS_KM;
        }
        if (radiusKm.compareTo(BigDecimal.ONE) < 0) {
            return BigDecimal.ONE;
        }
        if (radiusKm.compareTo(MAX_RADIUS_KM) > 0) {
            return MAX_RADIUS_KM;
        }
        return radiusKm;
    }

    /** price：亮点报价高者优先；默认按距离 */
    private Comparator<StoreNearbyVO> nearbyComparator(String sort) {
        if ("price".equalsIgnoreCase(sort)) {
            return Comparator.comparing(StoreNearbyVO::getHighlightPrice,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(StoreNearbyVO::getDistanceKm,
                            Comparator.nullsLast(Comparator.naturalOrder()));
        }
        return Comparator.comparing(StoreNearbyVO::getDistanceKm,
                Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private List<RecycleStation> visibleStations() {
        return stationMapper.selectList(new LambdaQueryWrapper<RecycleStation>()
                .eq(RecycleStation::getAuditStatus, "approved")
                .eq(RecycleStation::getStatus, 1));
    }

    private RecycleStation requireVisible(Long id) {
        RecycleStation s = stationMapper.selectById(id);
        if (s == null || s.getStatus() == null || s.getStatus() != 1
                || !"approved".equals(s.getAuditStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "门店不存在");
        }
        return s;
    }

    private Map<Long, Sku> enabledSkus(List<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        return skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                        .in(Sku::getId, skuIds)
                        .eq(Sku::getStatus, 1))
                .stream().collect(Collectors.toMap(Sku::getId, Function.identity()));
    }

    /** skuId -> 顶级分类 id */
    private Map<Long, Long> skuRootCategory(Iterable<Sku> skus) {
        Map<Long, Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, Long> result = new HashMap<>();
        for (Sku sku : skus) {
            Category c = categories.get(sku.getCategoryId());
            while (c != null && c.getParentId() != null && c.getParentId() != 0L) {
                c = categories.get(c.getParentId());
            }
            if (c != null) {
                result.put(sku.getId(), c.getId());
            }
        }
        return result;
    }

    /** 亮点品类：名称含「纸」或「塑」的顶级分类 */
    private List<Long> highlightRootCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, 0L))
                .stream()
                .filter(c -> c.getName() != null && (c.getName().contains("纸") || c.getName().contains("塑")))
                .map(Category::getId)
                .toList();
    }

    /** 纸类/塑料最高报价，无则取最高的一条报价（quotes 已按价格降序） */
    private BigDecimal highlightPrice(List<StationSkuPrice> quotes,
                                      Map<Long, Long> skuRoot, List<Long> highlightRoots) {
        if (quotes.isEmpty()) {
            return null;
        }
        return quotes.stream()
                .filter(q -> highlightRoots.contains(skuRoot.get(q.getSkuId())))
                .map(StationSkuPrice::getPrice)
                .findFirst()
                .orElse(quotes.get(0).getPrice());
    }

    /** 营业中且当前时间在营业时段内（时段无法解析时以营业状态为准） */
    private Boolean openNow(RecycleStation s) {
        if (s.getBusinessStatus() == null || s.getBusinessStatus() != 1) {
            return false;
        }
        String hours = s.getBusinessHours();
        if (!StringUtils.hasText(hours) || !hours.contains("-")) {
            return true;
        }
        try {
            String[] parts = hours.trim().split("-");
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());
            LocalTime now = LocalTime.now();
            if (start.isBefore(end)) {
                return !now.isBefore(start) && !now.isAfter(end);
            }
            // 跨零点时段（如 20:00-02:00）
            return !now.isBefore(start) || !now.isAfter(end);
        } catch (Exception e) {
            return true;
        }
    }
}
