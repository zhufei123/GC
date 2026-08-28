package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recycle.app.dto.BossPriceSaveDTO;
import com.recycle.app.dto.BossStoreUpdateDTO;
import com.recycle.app.dto.CompleteDTO;
import com.recycle.app.dto.WeighDTO;
import com.recycle.app.support.OrderAssembler;
import com.recycle.app.vo.BossPriceVO;
import com.recycle.app.vo.OrderVO;
import com.recycle.app.vo.WorkbenchVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.recycle.Category;
import com.recycle.common.entity.recycle.Sku;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.entity.store.StationSkuPrice;
import com.recycle.common.entity.trade.OrderItem;
import com.recycle.common.entity.trade.RecycleOrder;
import com.recycle.common.mapper.CategoryMapper;
import com.recycle.common.mapper.OrderItemMapper;
import com.recycle.common.mapper.RecycleOrderMapper;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.mapper.SkuMapper;
import com.recycle.common.mapper.StationSkuPriceMapper;
import com.recycle.common.support.SkuPriceReader;
import com.recycle.common.support.StationPriceReader;
import com.recycle.common.util.JsonUtils;
import com.recycle.common.util.QueryParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BossService {

    private final RecycleStationMapper stationMapper;
    private final RecycleOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final StationSkuPriceMapper stationSkuPriceMapper;
    private final SkuPriceReader skuPriceReader;
    private final StationPriceReader stationPriceReader;
    private final OrderAssembler orderAssembler;

    public RecycleStation myStation(Long bossId) {
        RecycleStation station = stationMapper.selectOne(new LambdaQueryWrapper<RecycleStation>()
                .eq(RecycleStation::getOwnerUserId, bossId));
        if (station == null) {
            throw new BizException(ErrorCode.STORE_NOT_APPROVED, "门店不存在，请先入驻");
        }
        return station;
    }

    public WorkbenchVO workbench(Long bossId) {
        RecycleStation station = myStation(bossId);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        WorkbenchVO vo = new WorkbenchVO();
        vo.setStoreId(station.getId());
        vo.setStoreName(station.getName());
        vo.setBusinessStatus(station.getBusinessStatus());
        vo.setAuditStatus(station.getAuditStatus());
        vo.setPendingPoolCount(orderMapper.selectCount(visiblePending(station.getId())));
        vo.setTodayAcceptedCount(orderMapper.selectCount(new LambdaQueryWrapper<RecycleOrder>()
                .eq(RecycleOrder::getStationId, station.getId())
                .ge(RecycleOrder::getAcceptedAt, todayStart)));
        List<RecycleOrder> todayCompleted = orderMapper.selectList(new LambdaQueryWrapper<RecycleOrder>()
                .eq(RecycleOrder::getStationId, station.getId())
                .eq(RecycleOrder::getStatus, "COMPLETED")
                .ge(RecycleOrder::getCompletedAt, todayStart));
        vo.setTodayCompletedCount(todayCompleted.size());
        vo.setTodayAmount(todayCompleted.stream()
                .map(RecycleOrder::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal todayWeight = BigDecimal.ZERO;
        if (!todayCompleted.isEmpty()) {
            todayWeight = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                            .in(OrderItem::getOrderId, todayCompleted.stream().map(RecycleOrder::getId).toList())
                            .eq(OrderItem::getItemType, "ACTUAL"))
                    .stream().map(OrderItem::getWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        vo.setTodayWeightKg(todayWeight);
        vo.setPendingCount(orderMapper.selectCount(new LambdaQueryWrapper<RecycleOrder>()
                .eq(RecycleOrder::getStationId, station.getId())
                .eq(RecycleOrder::getStatus, "ACCEPTED")));
        vo.setServingCount(orderMapper.selectCount(new LambdaQueryWrapper<RecycleOrder>()
                .eq(RecycleOrder::getStationId, station.getId())
                .eq(RecycleOrder::getStatus, "SERVING")));
        return vo;
    }

    /** 接单大厅：PENDING 上门单 + 指定本店的到店单 */
    public PageResult<OrderVO> pool(Long bossId, PageQuery query) {
        RecycleStation station = myStation(bossId);
        Page<RecycleOrder> page = orderMapper.selectPage(query.toPage(),
                visiblePending(station.getId()).orderByAsc(RecycleOrder::getCreateTime));
        return PageResult.of(page, o -> {
            // 带预估明细，供大厅展示品类摘要
            OrderVO vo = orderAssembler.toVO(o, true, true);
            vo.setReceiver(maskName(vo.getReceiver()));
            return vo;
        });
    }

    /** 抢单：上门单任意店可抢；到店单仅指定门店，乐观锁 WHERE status=PENDING */
    public void accept(Long bossId, Long orderId) {
        RecycleStation station = requireWorkableStation(bossId);
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<RecycleOrder>()
                .eq(RecycleOrder::getId, orderId)
                .eq(RecycleOrder::getStatus, "PENDING")
                .and(w -> w.isNull(RecycleOrder::getStationId)
                        .or()
                        .eq(RecycleOrder::getStationId, station.getId()))
                .set(RecycleOrder::getStatus, "ACCEPTED")
                .set(RecycleOrder::getStationId, station.getId())
                .set(RecycleOrder::getAcceptedAt, LocalDateTime.now()));
        if (rows == 0) {
            RecycleOrder order = orderMapper.selectById(orderId);
            if (order == null) {
                throw new BizException(ErrorCode.ORDER_NOT_FOUND);
            }
            if (!"PENDING".equals(order.getStatus())) {
                throw new BizException(ErrorCode.ORDER_TAKEN);
            }
            if (order.getStationId() != null && !order.getStationId().equals(station.getId())) {
                throw new BizException(ErrorCode.ORDER_NOT_FOUND, "非本店订单");
            }
            throw new BizException(ErrorCode.ORDER_TAKEN);
        }
    }

    /** ACCEPTED → SERVING */
    public void start(Long bossId, Long orderId) {
        RecycleStation station = myStation(bossId);
        requireStationOrder(station, orderId);
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<RecycleOrder>()
                .eq(RecycleOrder::getId, orderId)
                .eq(RecycleOrder::getStationId, station.getId())
                .eq(RecycleOrder::getStatus, "ACCEPTED")
                .set(RecycleOrder::getStatus, "SERVING")
                .set(RecycleOrder::getServedAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
    }

    /** 称重：实收明细 + 提交时刻本店报价快照，SERVING → WEIGHED */
    @Transactional
    public BigDecimal weigh(Long bossId, Long orderId, WeighDTO dto) {
        RecycleStation station = myStation(bossId);
        RecycleOrder order = requireStationOrder(station, orderId);
        if (!"SERVING".equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
        List<Long> skuIds = dto.getItems().stream().map(WeighDTO.WeighItem::getSkuId).toList();
        Map<Long, Sku> skus = skuMapper.selectByIds(skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        Map<Long, BigDecimal> prices = stationPriceReader.currentPrices(station.getId(), skuIds);

        // 重复称重覆盖旧实收明细
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .eq(OrderItem::getItemType, "ACTUAL"));

        BigDecimal total = BigDecimal.ZERO;
        for (WeighDTO.WeighItem itemDto : dto.getItems()) {
            Sku sku = skus.get(itemDto.getSkuId());
            if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
                throw new BizException(ErrorCode.SKU_OFFLINE);
            }
            BigDecimal price = prices.get(sku.getId());
            if (price == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "该回收站暂未报价该品类");
            }
            BigDecimal amount = price.multiply(itemDto.getWeight()).setScale(2, RoundingMode.HALF_UP);
            total = total.add(amount);

            OrderItem item = new OrderItem();
            item.setOrderId(orderId);
            item.setItemType("ACTUAL");
            item.setSkuId(sku.getId());
            item.setSkuName(sku.getName());
            item.setUnit(sku.getUnit());
            item.setWeight(itemDto.getWeight());
            item.setPrice(price);
            item.setAmount(amount);
            item.setCreateTime(LocalDateTime.now());
            orderItemMapper.insert(item);
        }

        int rows = orderMapper.update(null, new LambdaUpdateWrapper<RecycleOrder>()
                .eq(RecycleOrder::getId, orderId)
                .eq(RecycleOrder::getStatus, "SERVING")
                .set(RecycleOrder::getStatus, "WEIGHED")
                .set(RecycleOrder::getActualAmount, total)
                .set(RecycleOrder::getWeighedAt, LocalDateTime.now())
                .set(dto.getImages() != null && !dto.getImages().isEmpty(),
                        RecycleOrder::getPhotosWeigh, JsonUtils.toJson(dto.getImages())));
        if (rows == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
        return total;
    }

    /** WEIGHED → COMPLETED，确认金额必须与锁定金额一致 */
    public void complete(Long bossId, Long orderId, CompleteDTO dto) {
        RecycleStation station = myStation(bossId);
        RecycleOrder order = requireStationOrder(station, orderId);
        if (!"WEIGHED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
        if (order.getActualAmount() == null
                || dto.getConfirmAmount().compareTo(order.getActualAmount()) != 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "确认金额与锁定金额不一致");
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<RecycleOrder>()
                .eq(RecycleOrder::getId, orderId)
                .eq(RecycleOrder::getStatus, "WEIGHED")
                .set(RecycleOrder::getStatus, "COMPLETED")
                .set(RecycleOrder::getCompletedAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
    }

    public PageResult<OrderVO> orderPage(Long bossId, String status, PageQuery query) {
        RecycleStation station = myStation(bossId);
        String statusFilter = QueryParams.orderStatus(status);
        LambdaQueryWrapper<RecycleOrder> wrapper;
        if ("PENDING".equals(statusFilter)) {
            // 待接单：C 端下单已绑定本店（含历史未指派单），口径与接单大厅一致
            wrapper = visiblePending(station.getId());
        } else {
            wrapper = new LambdaQueryWrapper<RecycleOrder>()
                    .eq(RecycleOrder::getStationId, station.getId())
                    .eq(StringUtils.hasText(statusFilter), RecycleOrder::getStatus, statusFilter);
        }
        Page<RecycleOrder> page = orderMapper.selectPage(query.toPage(),
                wrapper.orderByDesc(RecycleOrder::getCreateTime));
        return PageResult.of(page, o -> orderAssembler.toVO(o, false, false));
    }

    public OrderVO orderDetail(Long bossId, Long orderId) {
        RecycleStation station = myStation(bossId);
        RecycleOrder order = requireStationOrder(station, orderId);
        return orderAssembler.toVO(order, true, false);
    }

    /** 报价管理：全部上架 SKU + 指导价 + 本店报价（无门店 403） */
    public List<BossPriceVO> prices(Long bossId) {
        RecycleStation station = stationOrForbidden(bossId);
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getStatus, 1)
                .orderByAsc(Sku::getSort));
        if (skus.isEmpty()) {
            return List.of();
        }
        List<Long> skuIds = skus.stream().map(Sku::getId).toList();
        Map<Long, BigDecimal> guidePrices = skuPriceReader.currentPrices(skuIds);
        Map<Long, StationSkuPrice> mine = stationSkuPriceMapper.selectList(
                        new LambdaQueryWrapper<StationSkuPrice>()
                                .eq(StationSkuPrice::getStationId, station.getId()))
                .stream()
                .collect(Collectors.toMap(StationSkuPrice::getSkuId, Function.identity(), (a, b) -> a));
        Map<Long, String> categoryNames = categoryMapper.selectByIds(
                        skus.stream().map(Sku::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(Category::getId, Category::getName));
        return skus.stream().map(sku -> {
            BossPriceVO vo = new BossPriceVO();
            vo.setSkuId(sku.getId());
            vo.setSkuName(sku.getName());
            vo.setUnit(sku.getUnit());
            vo.setCategoryName(categoryNames.get(sku.getCategoryId()));
            vo.setGuidePrice(guidePrices.get(sku.getId()));
            StationSkuPrice quote = mine.get(sku.getId());
            if (quote != null) {
                vo.setStationPrice(quote.getPrice());
                vo.setStatus(quote.getStatus());
                vo.setRemark(quote.getRemark());
            }
            return vo;
        }).toList();
    }

    /** 报价 upsert：报价中(status=1)必须 price > 0，status 仅 0/1（无门店 403） */
    @Transactional
    public void savePrices(Long bossId, BossPriceSaveDTO dto) {
        RecycleStation station = stationOrForbidden(bossId);
        List<Long> skuIds = dto.getItems().stream().map(BossPriceSaveDTO.Item::getSkuId).toList();
        Map<Long, Sku> skus = skuMapper.selectByIds(skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        Map<Long, StationSkuPrice> existing = stationSkuPriceMapper.selectList(
                        new LambdaQueryWrapper<StationSkuPrice>()
                                .eq(StationSkuPrice::getStationId, station.getId())
                                .in(StationSkuPrice::getSkuId, skuIds))
                .stream()
                .collect(Collectors.toMap(StationSkuPrice::getSkuId, Function.identity(), (a, b) -> a));
        for (BossPriceSaveDTO.Item item : dto.getItems()) {
            Sku sku = skus.get(item.getSkuId());
            if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
                throw new BizException(ErrorCode.SKU_OFFLINE);
            }
            int status = item.getStatus() == null ? 1 : item.getStatus();
            if (status != 0 && status != 1) {
                throw new BizException(ErrorCode.PARAM_ERROR, "报价状态仅允许 0/1");
            }
            boolean priceValid = item.getPrice() != null && item.getPrice().compareTo(BigDecimal.ZERO) > 0;
            if (status == 1 && !priceValid) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "「" + sku.getName() + "」报价必须大于 0");
            }
            StationSkuPrice row = existing.get(item.getSkuId());
            if (row == null) {
                // 停报且未填有效价：无可发布内容，跳过
                if (!priceValid) {
                    continue;
                }
                row = new StationSkuPrice();
                row.setStationId(station.getId());
                row.setSkuId(item.getSkuId());
                row.setPrice(item.getPrice());
                row.setStatus(status);
                row.setRemark(item.getRemark());
                stationSkuPriceMapper.insert(row);
            } else {
                if (priceValid) {
                    row.setPrice(item.getPrice());
                }
                row.setStatus(status);
                if (StringUtils.hasText(item.getRemark())) {
                    row.setRemark(item.getRemark());
                }
                stationSkuPriceMapper.updateById(row);
            }
        }
    }

    public RecycleStation getStore(Long bossId) {
        return myStation(bossId);
    }

    public void updateStore(Long bossId, BossStoreUpdateDTO dto) {
        RecycleStation station = myStation(bossId);
        if (StringUtils.hasText(dto.getName())) {
            station.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            station.setPhone(dto.getPhone());
        }
        if (StringUtils.hasText(dto.getContactName())) {
            station.setContactName(dto.getContactName());
        }
        if (StringUtils.hasText(dto.getProvince())) {
            station.setProvince(dto.getProvince());
        }
        if (StringUtils.hasText(dto.getCity())) {
            station.setCity(dto.getCity());
        }
        if (StringUtils.hasText(dto.getDistrict())) {
            station.setDistrict(dto.getDistrict());
        }
        if (StringUtils.hasText(dto.getAddress())) {
            station.setAddress(dto.getAddress());
        }
        if (dto.getLongitude() != null) {
            station.setLongitude(dto.getLongitude());
        }
        if (dto.getLatitude() != null) {
            station.setLatitude(dto.getLatitude());
        }
        if (StringUtils.hasText(dto.getBusinessHours())) {
            station.setBusinessHours(dto.getBusinessHours());
        }
        if (dto.getBusinessStatus() != null) {
            station.setBusinessStatus(dto.getBusinessStatus());
        }
        if (dto.getCategoryIds() != null) {
            station.setCategoryIds(JsonUtils.toJson(dto.getCategoryIds()));
        }
        if (dto.getPhotos() != null) {
            station.setPhotos(JsonUtils.toJson(dto.getPhotos()));
        }
        stationMapper.updateById(station);
    }

    /** 报价管理入口：未入驻门店返回 403 */
    private RecycleStation stationOrForbidden(Long bossId) {
        RecycleStation station = stationMapper.selectOne(new LambdaQueryWrapper<RecycleStation>()
                .eq(RecycleStation::getOwnerUserId, bossId));
        if (station == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "请先入驻门店");
        }
        return station;
    }

    private RecycleStation requireWorkableStation(Long bossId) {
        RecycleStation station = myStation(bossId);
        if (!"approved".equals(station.getAuditStatus()) || station.getStatus() == null || station.getStatus() != 1) {
            throw new BizException(ErrorCode.STORE_NOT_APPROVED);
        }
        if (station.getBusinessStatus() == null || station.getBusinessStatus() != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "门店休息中，不可接单");
        }
        return station;
    }

    private RecycleOrder requireStationOrder(RecycleStation station, Long orderId) {
        RecycleOrder order = orderMapper.selectById(orderId);
        if (order == null || !station.getId().equals(order.getStationId())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** PENDING 上门单（未指定门店）+ 指定本店的到店单 */
    private LambdaQueryWrapper<RecycleOrder> visiblePending(Long stationId) {
        return new LambdaQueryWrapper<RecycleOrder>()
                .eq(RecycleOrder::getStatus, "PENDING")
                .and(w -> w.isNull(RecycleOrder::getStationId)
                        .or()
                        .eq(RecycleOrder::getStationId, stationId));
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.charAt(0) + "**";
    }
}
