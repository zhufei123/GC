package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recycle.admin.dto.SkuPriceDTO;
import com.recycle.admin.dto.SkuSaveDTO;
import com.recycle.admin.vo.SkuPageVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.recycle.Category;
import com.recycle.common.entity.recycle.Sku;
import com.recycle.common.entity.recycle.SkuPrice;
import com.recycle.common.entity.recycle.SkuPriceLog;
import com.recycle.common.mapper.CategoryMapper;
import com.recycle.common.mapper.SkuMapper;
import com.recycle.common.mapper.SkuPriceLogMapper;
import com.recycle.common.mapper.SkuPriceMapper;
import com.recycle.common.support.SkuPriceReader;
import com.recycle.common.util.QueryParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSkuService {

    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final SkuPriceMapper priceMapper;
    private final SkuPriceLogMapper priceLogMapper;
    private final SkuPriceReader priceReader;

    public PageResult<SkuPageVO> page(Long categoryId, String name, Integer status, PageQuery query) {
        String keyword = QueryParams.firstText(query.getKeyword(), name);
        Page<Sku> page = skuMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Sku>()
                        .eq(categoryId != null, Sku::getCategoryId, categoryId)
                        .like(StringUtils.hasText(keyword), Sku::getName, keyword)
                        .eq(status != null, Sku::getStatus, status)
                        .orderByAsc(Sku::getSort)
                        .orderByAsc(Sku::getId));
        List<Long> skuIds = page.getRecords().stream().map(Sku::getId).toList();
        Map<Long, BigDecimal> prices = priceReader.currentPrices(skuIds);
        Map<Long, String> categoryNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        return PageResult.of(page, sku -> toVO(sku, prices.get(sku.getId()),
                categoryNames.get(sku.getCategoryId())));
    }

    public SkuPageVO detail(Long id) {
        Sku sku = require(id);
        Category category = categoryMapper.selectById(sku.getCategoryId());
        return toVO(sku, priceReader.currentPrice(id), category == null ? null : category.getName());
    }

    @Transactional
    public Long create(SkuSaveDTO dto) {
        requireCategory(dto.getCategoryId());
        Sku sku = new Sku();
        copy(dto, sku);
        skuMapper.insert(sku);
        if (dto.getPrice() != null) {
            insertPriceWithLog(sku.getId(), null, dto.getPrice(), LocalDateTime.now(), "初始定价", null);
        }
        return sku.getId();
    }

    public void update(Long id, SkuSaveDTO dto) {
        Sku sku = require(id);
        requireCategory(dto.getCategoryId());
        copy(dto, sku);
        skuMapper.updateById(sku);
    }

    public void delete(Long id) {
        require(id);
        skuMapper.deleteById(id);
    }

    public void updateStatus(Long id, Integer status) {
        Sku sku = require(id);
        sku.setStatus(status);
        skuMapper.updateById(sku);
    }

    /** 改价：写 sku_price + sku_price_log */
    @Transactional
    public void changePrice(Long id, SkuPriceDTO dto, Long operatorId) {
        require(id);
        BigDecimal oldPrice = priceReader.currentPrice(id);
        LocalDateTime effectiveAt = dto.getEffectiveAt() == null ? LocalDateTime.now() : dto.getEffectiveAt();
        insertPriceWithLog(id, oldPrice, dto.getPrice(), effectiveAt, dto.getReason(), operatorId);
    }

    public List<SkuPriceLog> priceLog(Long id) {
        require(id);
        return priceLogMapper.selectList(new LambdaQueryWrapper<SkuPriceLog>()
                .eq(SkuPriceLog::getSkuId, id)
                .orderByDesc(SkuPriceLog::getCreateTime)
                .orderByDesc(SkuPriceLog::getId));
    }

    private void insertPriceWithLog(Long skuId, BigDecimal oldPrice, BigDecimal newPrice,
                                    LocalDateTime effectiveAt, String reason, Long operatorId) {
        SkuPrice price = new SkuPrice();
        price.setSkuId(skuId);
        price.setCityCode("ALL");
        price.setPrice(newPrice);
        price.setEffectiveAt(effectiveAt);
        price.setStatus(1);
        priceMapper.insert(price);

        SkuPriceLog log = new SkuPriceLog();
        log.setSkuId(skuId);
        log.setOldPrice(oldPrice);
        log.setNewPrice(newPrice);
        log.setEffectiveAt(effectiveAt);
        log.setReason(reason);
        log.setOperatorId(operatorId);
        log.setCreateTime(LocalDateTime.now());
        priceLogMapper.insert(log);
    }

    private Sku require(Long id) {
        Sku sku = skuMapper.selectById(id);
        if (sku == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "SKU 不存在");
        }
        return sku;
    }

    private void requireCategory(Long categoryId) {
        if (categoryId != null && categoryMapper.selectById(categoryId) == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "分类不存在");
        }
    }

    private void copy(SkuSaveDTO dto, Sku sku) {
        sku.setCategoryId(dto.getCategoryId());
        sku.setName(dto.getName());
        sku.setImage(dto.getImage());
        sku.setUnit(StringUtils.hasText(dto.getUnit()) ? dto.getUnit() : "kg");
        sku.setDescription(dto.getDescription());
        sku.setSort(dto.getSort() == null ? 0 : dto.getSort());
        sku.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private SkuPageVO toVO(Sku sku, BigDecimal price, String categoryName) {
        SkuPageVO vo = new SkuPageVO();
        vo.setId(sku.getId());
        vo.setCategoryId(sku.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setName(sku.getName());
        vo.setImage(sku.getImage());
        vo.setUnit(sku.getUnit());
        vo.setDescription(sku.getDescription());
        vo.setSort(sku.getSort());
        vo.setStatus(sku.getStatus());
        vo.setPrice(price);
        vo.setCreateTime(sku.getCreateTime());
        return vo;
    }
}
