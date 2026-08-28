package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recycle.app.dto.OrderCancelDTO;
import com.recycle.app.dto.OrderCreateDTO;
import com.recycle.app.dto.OrderReviewDTO;
import com.recycle.app.support.OrderAssembler;
import com.recycle.app.vo.OrderReviewVO;
import com.recycle.app.vo.OrderVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.member.UserAddress;
import com.recycle.common.entity.recycle.Sku;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.entity.trade.OrderItem;
import com.recycle.common.entity.trade.OrderReview;
import com.recycle.common.entity.trade.RecycleOrder;
import com.recycle.common.mapper.OrderItemMapper;
import com.recycle.common.mapper.OrderReviewMapper;
import com.recycle.common.mapper.RecycleOrderMapper;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.mapper.SkuMapper;
import com.recycle.common.mapper.UserAddressMapper;
import com.recycle.common.support.StationPriceReader;
import com.recycle.common.util.JsonUtils;
import com.recycle.common.util.QueryParams;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppOrderService {

    private static final List<String> ACTIVE_STATUSES =
            List.of("PENDING", "ACCEPTED", "SERVING", "WEIGHED");
    private static final int MAX_ACTIVE_ORDERS = 5;

    private final RecycleOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderReviewMapper orderReviewMapper;
    private final UserAddressMapper addressMapper;
    private final RecycleStationMapper stationMapper;
    private final SkuMapper skuMapper;
    private final StationPriceReader stationPriceReader;
    private final OrderAssembler orderAssembler;
    private final NotifyService notifyService;

    @Transactional
    public Long create(Long userId, OrderCreateDTO dto) {
        // requestId 幂等：仅对本用户生效
        if (StringUtils.hasText(dto.getRequestId())) {
            RecycleOrder existing = orderMapper.selectOne(new LambdaQueryWrapper<RecycleOrder>()
                    .eq(RecycleOrder::getUserId, userId)
                    .eq(RecycleOrder::getRequestId, dto.getRequestId()));
            if (existing != null) {
                return existing.getId();
            }
        }
        Long activeCount = orderMapper.selectCount(new LambdaQueryWrapper<RecycleOrder>()
                .eq(RecycleOrder::getUserId, userId)
                .in(RecycleOrder::getStatus, ACTIVE_STATUSES));
        if (activeCount >= MAX_ACTIVE_ORDERS) {
            throw new BizException(ErrorCode.PARAM_ERROR, "进行中订单已达上限 " + MAX_ACTIVE_ORDERS + " 单");
        }

        RecycleOrder order = new RecycleOrder();
        order.setOrderNo(genOrderNo());
        order.setUserId(userId);
        String type = StringUtils.hasText(dto.getType()) ? dto.getType() : "PICKUP";
        if (!"PICKUP".equals(type) && !"DROPOFF".equals(type)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "订单类型无效");
        }
        order.setType(type);
        order.setStatus("PENDING");
        order.setAppointDate(dto.getAppointDate());
        order.setAppointPeriod(dto.getAppointPeriod());
        order.setRemark(dto.getRemark());
        order.setRequestId(StringUtils.hasText(dto.getRequestId()) ? dto.getRequestId() : null);
        order.setActualAmount(BigDecimal.ZERO);
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            order.setPhotosCustomer(JsonUtils.toJson(dto.getImages()));
        }

        // 上门/到店均须选定回收站：按门店报价成交
        if (dto.getStoreId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择回收站");
        }
        RecycleStation store = stationMapper.selectById(dto.getStoreId());
        if (store == null || store.getStatus() == null || store.getStatus() != 1
                || !"approved".equals(store.getAuditStatus())) {
            throw new BizException(ErrorCode.STORE_NOT_APPROVED, "门店不存在或未营业");
        }
        order.setStationId(store.getId());

        if ("PICKUP".equals(order.getType())) {
            if (dto.getAddressId() == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "上门单必须选择地址");
            }
            UserAddress address = addressMapper.selectById(dto.getAddressId());
            if (address == null || !address.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.NOT_FOUND, "地址不存在");
            }
            order.setReceiver(address.getReceiver());
            order.setPhone(address.getPhone());
            order.setAddress(joinAddress(address));
            order.setLongitude(address.getLongitude());
            order.setLatitude(address.getLatitude());
        } else if ("DROPOFF".equals(order.getType())) {
            order.setReceiver(store.getContactName());
            order.setPhone(store.getPhone());
            order.setAddress(joinStoreAddress(store));
            order.setLongitude(store.getLongitude());
            order.setLatitude(store.getLatitude());
        }

        // 预估明细 + 门店当前报价快照
        List<Long> skuIds = dto.getEstimateItems().stream()
                .map(OrderCreateDTO.EstimateItem::getSkuId).toList();
        Map<Long, Sku> skus = skuMapper.selectByIds(skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        Map<Long, BigDecimal> prices = stationPriceReader.currentPrices(store.getId(), skuIds);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal estimateAmount = BigDecimal.ZERO;
        for (OrderCreateDTO.EstimateItem itemDto : dto.getEstimateItems()) {
            Sku sku = skus.get(itemDto.getSkuId());
            if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
                throw new BizException(ErrorCode.SKU_OFFLINE);
            }
            BigDecimal price = prices.get(sku.getId());
            if (price == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "该回收站暂未报价该品类");
            }
            BigDecimal weight = itemDto.getEstimateWeight();
            if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.PARAM_ERROR, "预估重量必须大于 0");
            }
            BigDecimal amount = price.multiply(weight).setScale(2, RoundingMode.HALF_UP);
            estimateAmount = estimateAmount.add(amount);

            OrderItem item = new OrderItem();
            item.setItemType("ESTIMATE");
            item.setSkuId(sku.getId());
            item.setSkuName(sku.getName());
            item.setUnit(sku.getUnit());
            item.setWeight(weight);
            item.setPrice(price);
            item.setAmount(amount);
            item.setCreateTime(LocalDateTime.now());
            items.add(item);
        }
        order.setEstimateAmount(estimateAmount);
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            if (StringUtils.hasText(dto.getRequestId())) {
                RecycleOrder existing = orderMapper.selectOne(new LambdaQueryWrapper<RecycleOrder>()
                        .eq(RecycleOrder::getUserId, userId)
                        .eq(RecycleOrder::getRequestId, dto.getRequestId()));
                if (existing != null) {
                    return existing.getId();
                }
            }
            throw e;
        }
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        notifyService.inAppQuietly(userId, "ORDER_CREATED", "预约成功",
                "回收订单 " + order.getOrderNo() + " 已提交，等待回收站接单", "ORDER", order.getId());
        return order.getId();
    }

    public PageResult<OrderVO> page(Long userId, String status, PageQuery query) {
        String statusFilter = QueryParams.orderStatus(status);
        Page<RecycleOrder> page = orderMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<RecycleOrder>()
                        .eq(RecycleOrder::getUserId, userId)
                        .eq(StringUtils.hasText(statusFilter), RecycleOrder::getStatus, statusFilter)
                        .orderByDesc(RecycleOrder::getCreateTime));
        return PageResult.of(page, o -> orderAssembler.toVO(o, false, false));
    }

    public OrderVO detail(Long userId, Long orderId) {
        RecycleOrder order = requireOwn(userId, orderId);
        return orderAssembler.toVO(order, true, false);
    }

    public void cancel(Long userId, Long orderId, OrderCancelDTO dto) {
        RecycleOrder order = requireOwn(userId, orderId);
        if (!"PENDING".equals(order.getStatus()) && !"ACCEPTED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<RecycleOrder>()
                .eq(RecycleOrder::getId, orderId)
                .in(RecycleOrder::getStatus, List.of("PENDING", "ACCEPTED"))
                .set(RecycleOrder::getStatus, "CANCELLED")
                .set(RecycleOrder::getCancelBy, "customer")
                .set(RecycleOrder::getCancelReason, dto.getReason())
                .set(RecycleOrder::getCancelledAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
        notifyService.inAppQuietly(userId, "ORDER_CANCELLED", "订单已取消",
                "订单 " + order.getOrderNo() + " 已取消", "ORDER", orderId);
    }

    /** 评价：仅 COMPLETED 订单，一单一评（uk_review_order 兜底并发） */
    public void review(Long userId, Long orderId, OrderReviewDTO dto) {
        RecycleOrder order = requireOwn(userId, orderId);
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL, "订单完成后才能评价");
        }
        OrderReview review = new OrderReview();
        review.setOrderId(orderId);
        review.setUserId(userId);
        review.setStationId(order.getStationId());
        review.setRating(dto.getRating());
        review.setComment(StringUtils.hasText(dto.getComment()) ? dto.getComment().trim() : null);
        try {
            orderReviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该订单已评价");
        }
    }

    /** 我的订单评价，未评价返回 null */
    public OrderReviewVO getReview(Long userId, Long orderId) {
        requireOwn(userId, orderId);
        OrderReview review = orderReviewMapper.selectOne(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getOrderId, orderId));
        if (review == null) {
            return null;
        }
        OrderReviewVO vo = new OrderReviewVO();
        vo.setOrderId(review.getOrderId());
        vo.setRating(review.getRating());
        vo.setComment(review.getComment());
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }

    private RecycleOrder requireOwn(Long userId, Long orderId) {
        RecycleOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private String joinAddress(UserAddress a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a.getProvince()).append(a.getCity()).append(a.getDistrict());
        if (StringUtils.hasText(a.getStreet())) {
            sb.append(a.getStreet());
        }
        sb.append(a.getDetail());
        return sb.toString();
    }

    private String joinStoreAddress(RecycleStation s) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(s.getProvince())) {
            sb.append(s.getProvince());
        }
        if (StringUtils.hasText(s.getCity())) {
            sb.append(s.getCity());
        }
        if (StringUtils.hasText(s.getDistrict())) {
            sb.append(s.getDistrict());
        }
        if (StringUtils.hasText(s.getAddress())) {
            sb.append(s.getAddress());
        }
        return sb.toString();
    }

    private String genOrderNo() {
        return "RO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
