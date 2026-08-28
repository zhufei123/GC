package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.admin.dto.AuditDTO;
import com.recycle.admin.vo.AdminStorePriceVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.member.User;
import com.recycle.common.entity.recycle.Category;
import com.recycle.common.entity.recycle.Sku;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.entity.store.StationApply;
import com.recycle.common.entity.store.StationSkuPrice;
import com.recycle.common.mapper.CategoryMapper;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.mapper.SkuMapper;
import com.recycle.common.mapper.StationApplyMapper;
import com.recycle.common.mapper.StationSkuPriceMapper;
import com.recycle.common.mapper.UserMapper;
import com.recycle.common.support.SkuPriceReader;
import com.recycle.common.support.StationPriceSeeder;
import com.recycle.common.util.QueryParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStoreService {

    private final RecycleStationMapper stationMapper;
    private final StationApplyMapper applyMapper;
    private final UserMapper userMapper;
    private final StationSkuPriceMapper stationSkuPriceMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final SkuPriceReader skuPriceReader;
    private final StationPriceSeeder stationPriceSeeder;

    public PageResult<RecycleStation> storePage(String name, Integer status, PageQuery query) {
        String keyword = QueryParams.firstText(query.getKeyword(), name);
        return PageResult.of(stationMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<RecycleStation>()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(RecycleStation::getName, keyword)
                                .or()
                                .like(RecycleStation::getPhone, keyword)
                                .or()
                                .like(RecycleStation::getContactName, keyword)
                                .or()
                                .like(RecycleStation::getAddress, keyword))
                        .eq(status != null, RecycleStation::getStatus, status)
                        .orderByDesc(RecycleStation::getId)));
    }

    public void updateStore(Long id, RecycleStation store) {
        requireStore(id);
        store.setId(id);
        store.setOwnerUserId(null);
        stationMapper.updateById(store);
    }

    public void updateStoreStatus(Long id, Integer status) {
        RecycleStation store = requireStore(id);
        store.setStatus(status);
        stationMapper.updateById(store);
    }

    public PageResult<StationApply> applyPage(String auditStatus, PageQuery query) {
        String status = QueryParams.lower(auditStatus);
        return PageResult.of(applyMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<StationApply>()
                        .eq(StringUtils.hasText(status), StationApply::getAuditStatus, status)
                        .orderByDesc(StationApply::getId)));
    }

    public StationApply applyDetail(Long id) {
        return requireApply(id);
    }

    /** 审核：通过 → 建 recycle_station + 用户升级 recycler */
    @Transactional
    public void audit(Long id, AuditDTO dto, Long auditorId) {
        StationApply apply = requireApply(id);
        if (!"pending".equals(apply.getAuditStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该申请已审核");
        }
        User user = userMapper.selectById(apply.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        apply.setAuditStatus(dto.getPass() ? "approved" : "rejected");
        apply.setAuditRemark(dto.getRemark());
        apply.setAuditorId(auditorId);
        apply.setAuditTime(LocalDateTime.now());
        applyMapper.updateById(apply);

        if (dto.getPass()) {
            user.setRole("recycler");
            user.setRecyclerStatus("approved");
            userMapper.updateById(user);

            RecycleStation existing = stationMapper.selectOne(new LambdaQueryWrapper<RecycleStation>()
                    .eq(RecycleStation::getOwnerUserId, user.getId()));
            if (existing == null) {
                RecycleStation station = new RecycleStation();
                station.setOwnerUserId(user.getId());
                station.setName(apply.getStoreName());
                station.setPhone(apply.getContactPhone());
                station.setContactName(apply.getContactName());
                station.setProvince(apply.getProvince());
                station.setCity(apply.getCity());
                station.setDistrict(apply.getDistrict());
                station.setAddress(apply.getDetail());
                station.setLongitude(apply.getLongitude());
                station.setLatitude(apply.getLatitude());
                station.setPhotos(apply.getStoreImages());
                station.setCategoryIds(apply.getCategoryIds());
                station.setBusinessHours("09:00-18:00");
                station.setBusinessStatus(1);
                station.setAuditStatus("approved");
                station.setStatus(1);
                stationMapper.insert(station);
                stationPriceSeeder.seedFromGuideIfEmpty(station.getId());
            } else {
                stationPriceSeeder.seedFromGuideIfEmpty(existing.getId());
            }
        } else {
            user.setRecyclerStatus("rejected");
            userMapper.updateById(user);
        }
    }

    /** 管理端查看某站报价，对照平台指导价 */
    public List<AdminStorePriceVO> storePrices(Long id) {
        requireStore(id);
        List<StationSkuPrice> rows = stationSkuPriceMapper.selectList(
                new LambdaQueryWrapper<StationSkuPrice>()
                        .eq(StationSkuPrice::getStationId, id));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> skuIds = rows.stream().map(StationSkuPrice::getSkuId).distinct().toList();
        Map<Long, Sku> skus = skuMapper.selectByIds(skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));
        Map<Long, String> categoryNames = categoryMapper.selectByIds(
                        skus.values().stream().map(Sku::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        Map<Long, BigDecimal> guidePrices = skuPriceReader.currentPrices(skuIds);
        return rows.stream()
                .filter(r -> skus.containsKey(r.getSkuId()))
                .sorted(Comparator.comparing(StationSkuPrice::getStatus, Comparator.reverseOrder())
                        .thenComparing(StationSkuPrice::getPrice, Comparator.reverseOrder()))
                .map(r -> {
                    Sku sku = skus.get(r.getSkuId());
                    AdminStorePriceVO vo = new AdminStorePriceVO();
                    vo.setSkuId(sku.getId());
                    vo.setSkuName(sku.getName());
                    vo.setUnit(sku.getUnit());
                    vo.setCategoryName(categoryNames.get(sku.getCategoryId()));
                    vo.setPrice(r.getPrice());
                    vo.setStatus(r.getStatus());
                    vo.setGuidePrice(guidePrices.get(sku.getId()));
                    return vo;
                })
                .toList();
    }

    private RecycleStation requireStore(Long id) {
        RecycleStation store = stationMapper.selectById(id);
        if (store == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "门店不存在");
        }
        return store;
    }

    private StationApply requireApply(Long id) {
        StationApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "入驻申请不存在");
        }
        return apply;
    }
}
