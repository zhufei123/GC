package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.app.dto.StoreApplyDTO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.entity.member.User;
import com.recycle.common.entity.store.StationApply;
import com.recycle.common.mapper.StationApplyMapper;
import com.recycle.common.mapper.UserMapper;
import com.recycle.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppStoreApplyService {

    private final StationApplyMapper applyMapper;
    private final UserMapper userMapper;

    @Transactional
    public Long apply(Long userId, StoreApplyDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if ("recycler".equals(user.getRole())) {
            throw new BizException(ErrorCode.APPLY_DUPLICATE, "已是回收商，无需重复申请");
        }
        Long pending = applyMapper.selectCount(new LambdaQueryWrapper<StationApply>()
                .eq(StationApply::getUserId, userId)
                .eq(StationApply::getAuditStatus, "pending"));
        if (pending > 0) {
            throw new BizException(ErrorCode.APPLY_DUPLICATE);
        }
        StationApply apply = new StationApply();
        apply.setUserId(userId);
        apply.setStoreName(dto.getStoreName());
        apply.setContactName(dto.getContactName());
        apply.setContactPhone(dto.getContactPhone());
        apply.setProvince(dto.getProvince());
        apply.setCity(dto.getCity());
        apply.setDistrict(dto.getDistrict());
        apply.setDetail(dto.getDetail());
        apply.setLongitude(dto.getLongitude());
        apply.setLatitude(dto.getLatitude());
        apply.setLicenseImage(dto.getLicenseImage());
        if (dto.getStoreImages() != null && !dto.getStoreImages().isEmpty()) {
            apply.setStoreImages(JsonUtils.toJson(dto.getStoreImages()));
        }
        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            apply.setCategoryIds(JsonUtils.toJson(dto.getCategoryIds()));
        }
        apply.setAuditStatus("pending");
        applyMapper.insert(apply);

        user.setRecyclerStatus("pending");
        userMapper.updateById(user);
        return apply.getId();
    }

    public StationApply latest(Long userId) {
        return applyMapper.selectOne(new LambdaQueryWrapper<StationApply>()
                .eq(StationApply::getUserId, userId)
                .orderByDesc(StationApply::getCreateTime)
                .orderByDesc(StationApply::getId)
                .last("LIMIT 1"));
    }
}
