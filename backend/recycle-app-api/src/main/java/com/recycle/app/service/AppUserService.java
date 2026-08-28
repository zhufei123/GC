package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recycle.app.dto.AddressDTO;
import com.recycle.app.dto.ProfileDTO;
import com.recycle.app.vo.FavoriteStationVO;
import com.recycle.app.vo.UserMeVO;
import com.recycle.app.vo.WalletVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.member.NotifyLog;
import com.recycle.common.entity.member.User;
import com.recycle.common.entity.member.UserAddress;
import com.recycle.common.entity.member.UserFavoriteStation;
import com.recycle.common.entity.member.WalletLedger;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.mapper.NotifyLogMapper;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.mapper.UserAddressMapper;
import com.recycle.common.mapper.UserFavoriteStationMapper;
import com.recycle.common.mapper.UserMapper;
import com.recycle.common.mapper.WalletLedgerMapper;
import com.recycle.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final UserMapper userMapper;
    private final UserAddressMapper addressMapper;
    private final RecycleStationMapper stationMapper;
    private final UserFavoriteStationMapper favoriteMapper;
    private final WalletLedgerMapper walletLedgerMapper;
    private final NotifyLogMapper notifyLogMapper;

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

    /** 更新昵称/头像 */
    public void updateProfile(Long userId, ProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar(dto.getAvatar());
        }
        userMapper.updateById(user);
    }

    /** 钱包：余额 + 最近流水 */
    public WalletVO wallet(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        WalletVO vo = new WalletVO();
        vo.setBalance(user.getBalance() == null ? BigDecimal.ZERO : user.getBalance());
        vo.setList(walletLedgerMapper.selectList(new LambdaQueryWrapper<WalletLedger>()
                .eq(WalletLedger::getUserId, userId)
                .orderByDesc(WalletLedger::getId)
                .last("LIMIT 20")));
        return vo;
    }

    /** 我的消息分页（notify_log） */
    public PageResult<NotifyLog> notices(Long userId, PageQuery query) {
        Page<NotifyLog> page = notifyLogMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<NotifyLog>()
                        .eq(NotifyLog::getUserId, userId)
                        .orderByDesc(NotifyLog::getId));
        return PageResult.of(page);
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

    /* ---------- 收藏回收站 ---------- */

    public boolean isFavorite(Long userId, Long stationId) {
        return favoriteMapper.exists(new LambdaQueryWrapper<UserFavoriteStation>()
                .eq(UserFavoriteStation::getUserId, userId)
                .eq(UserFavoriteStation::getStationId, stationId));
    }

    public void addFavorite(Long userId, Long stationId) {
        RecycleStation station = stationMapper.selectById(stationId);
        if (station == null || !"approved".equals(station.getAuditStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "门店不存在");
        }
        UserFavoriteStation fav = new UserFavoriteStation();
        fav.setUserId(userId);
        fav.setStationId(stationId);
        try {
            favoriteMapper.insert(fav);
        } catch (DuplicateKeyException e) {
            // 已收藏，幂等成功
        }
    }

    public void removeFavorite(Long userId, Long stationId) {
        favoriteMapper.delete(new LambdaQueryWrapper<UserFavoriteStation>()
                .eq(UserFavoriteStation::getUserId, userId)
                .eq(UserFavoriteStation::getStationId, stationId));
    }

    /** 收藏列表（新收藏在前），门店被删除/下线时标记 available=false */
    public List<FavoriteStationVO> listFavorites(Long userId) {
        List<UserFavoriteStation> favs = favoriteMapper.selectList(
                new LambdaQueryWrapper<UserFavoriteStation>()
                        .eq(UserFavoriteStation::getUserId, userId)
                        .orderByDesc(UserFavoriteStation::getId));
        if (favs.isEmpty()) {
            return List.of();
        }
        Map<Long, RecycleStation> stations = stationMapper.selectByIds(
                        favs.stream().map(UserFavoriteStation::getStationId).toList())
                .stream().collect(Collectors.toMap(RecycleStation::getId, Function.identity()));
        return favs.stream().map(f -> {
            FavoriteStationVO vo = new FavoriteStationVO();
            vo.setId(f.getStationId());
            vo.setFavoritedAt(f.getCreateTime());
            RecycleStation s = stations.get(f.getStationId());
            if (s == null) {
                vo.setName("门店已下线");
                vo.setAvailable(false);
                return vo;
            }
            vo.setName(s.getName());
            vo.setAddress(s.getAddress());
            vo.setPhone(s.getPhone());
            vo.setBusinessHours(s.getBusinessHours());
            vo.setBusinessStatus(s.getBusinessStatus());
            vo.setLongitude(s.getLongitude());
            vo.setLatitude(s.getLatitude());
            vo.setPhotos(JsonUtils.toStringList(s.getPhotos()));
            vo.setAvailable("approved".equals(s.getAuditStatus())
                    && s.getStatus() != null && s.getStatus() == 1);
            return vo;
        }).toList();
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
