package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.recycle.app.dto.AddressDTO;
import com.recycle.app.vo.UserMeVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.entity.member.User;
import com.recycle.common.entity.member.UserAddress;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.mapper.UserAddressMapper;
import com.recycle.common.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final UserMapper userMapper;
    private final UserAddressMapper addressMapper;
    private final RecycleStationMapper stationMapper;

    public UserMeVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        UserMeVO vo = new UserMeVO();
        vo.setUserId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setRecyclerStatus(user.getRecyclerStatus());
        vo.setHasPhone(StringUtils.hasText(user.getPhone()));
        if ("recycler".equals(user.getRole())) {
            RecycleStation station = stationMapper.selectOne(
                    new LambdaQueryWrapper<RecycleStation>().eq(RecycleStation::getOwnerUserId, user.getId()));
            if (station != null) {
                vo.setStoreId(station.getId());
            }
        }
        return vo;
    }

    public List<UserAddress> listAddress(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getId));
    }

    @Transactional
    public Long addAddress(Long userId, AddressDTO dto) {
        UserAddress address = new UserAddress();
        copy(dto, address);
        address.setUserId(userId);
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefault(userId);
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }
        addressMapper.insert(address);
        return address.getId();
    }

    @Transactional
    public void updateAddress(Long userId, Long id, AddressDTO dto) {
        UserAddress address = requireOwn(userId, id);
        copy(dto, address);
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefault(userId);
            address.setIsDefault(1);
        } else if (Boolean.FALSE.equals(dto.getIsDefault())) {
            address.setIsDefault(0);
        }
        addressMapper.updateById(address);
    }

    public void deleteAddress(Long userId, Long id) {
        requireOwn(userId, id);
        addressMapper.deleteById(id);
    }

    @Transactional
    public void setDefault(Long userId, Long id) {
        requireOwn(userId, id);
        clearDefault(userId);
        addressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getId, id)
                .set(UserAddress::getIsDefault, 1));
    }

    public UserAddress requireOwn(Long userId, Long id) {
        UserAddress address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "地址不存在");
        }
        return address;
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .set(UserAddress::getIsDefault, 0));
    }

    private void copy(AddressDTO dto, UserAddress address) {
        address.setReceiver(dto.getReceiver());
        address.setPhone(dto.getPhone());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setStreet(dto.getStreet());
        address.setDetail(dto.getDetail());
        address.setLongitude(dto.getLongitude());
        address.setLatitude(dto.getLatitude());
    }
}
