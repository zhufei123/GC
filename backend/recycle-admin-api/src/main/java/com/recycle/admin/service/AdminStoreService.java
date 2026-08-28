package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.admin.dto.AuditDTO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.member.User;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.entity.store.StationApply;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.mapper.StationApplyMapper;
import com.recycle.common.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminStoreService {

    private final RecycleStationMapper stationMapper;
    private final StationApplyMapper applyMapper;
    private final UserMapper userMapper;

    public PageResult<RecycleStation> storePage(String name, Integer status, PageQuery query) {
        return PageResult.of(stationMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<RecycleStation>()
                        .like(StringUtils.hasText(name), RecycleStation::getName, name)
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
        return PageResult.of(applyMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<StationApply>()
                        .eq(StringUtils.hasText(auditStatus), StationApply::getAuditStatus, auditStatus)
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
            }
        } else {
            user.setRecyclerStatus("rejected");
            userMapper.updateById(user);
        }
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
