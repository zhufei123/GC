package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.admin.dto.CategorySaveDTO;
import com.recycle.admin.vo.CategoryTreeVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.entity.recycle.Category;
import com.recycle.common.entity.recycle.Sku;
import com.recycle.common.mapper.CategoryMapper;
import com.recycle.common.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryMapper categoryMapper;
    private final SkuMapper skuMapper;

    /** 全量分类树（含停用） */
    public List<CategoryTreeVO> tree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        Map<Long, CategoryTreeVO> nodes = new LinkedHashMap<>();
        for (Category c : all) {
            CategoryTreeVO node = new CategoryTreeVO();
            node.setId(c.getId());
            node.setParentId(c.getParentId());
            node.setName(c.getName());
            node.setIcon(c.getIcon());
            node.setSort(c.getSort());
            node.setStatus(c.getStatus());
            node.setCreateTime(c.getCreateTime());
            nodes.put(c.getId(), node);
        }
        List<CategoryTreeVO> roots = new ArrayList<>();
        for (CategoryTreeVO node : nodes.values()) {
            CategoryTreeVO parent = nodes.get(node.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        Comparator<CategoryTreeVO> bySort = Comparator.comparing(CategoryTreeVO::getSort,
                Comparator.nullsLast(Comparator.naturalOrder()));
        nodes.values().forEach(n -> n.getChildren().sort(bySort));
        roots.sort(bySort);
        return roots;
    }

    public Long create(CategorySaveDTO dto) {
        Category category = new Category();
        copy(dto, category);
        categoryMapper.insert(category);
        return category.getId();
    }

    public void update(Long id, CategorySaveDTO dto) {
        Category category = require(id);
        copy(dto, category);
        categoryMapper.updateById(category);
    }

    /** 有子级或 SKU → 30001 */
    public void delete(Long id) {
        require(id);
        Long children = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        Long skus = skuMapper.selectCount(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getCategoryId, id));
        if (children > 0 || skus > 0) {
            throw new BizException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
        categoryMapper.deleteById(id);
    }

    public void updateStatus(Long id, Integer status) {
        Category category = require(id);
        category.setStatus(status);
        categoryMapper.updateById(category);
    }

    private Category require(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private void copy(CategorySaveDTO dto, Category category) {
        category.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }
}
