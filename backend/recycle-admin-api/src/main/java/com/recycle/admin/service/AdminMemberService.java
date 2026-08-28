package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.admin.vo.UserPageVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.member.User;
import com.recycle.common.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final UserMapper userMapper;

    public PageResult<UserPageVO> page(String phone, String role, Integer status, PageQuery query) {
        return PageResult.of(userMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<User>()
                        .like(StringUtils.hasText(phone), User::getPhone, phone)
                        .eq(StringUtils.hasText(role), User::getRole, role)
                        .eq(status != null, User::getStatus, status)
                        .orderByDesc(User::getId)), this::toVO);
    }

    public UserPageVO detail(Long id) {
        return toVO(require(id));
    }

    public void updateStatus(Long id, Integer status) {
        User user = require(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private User require(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserPageVO toVO(User user) {
        UserPageVO vo = new UserPageVO();
        vo.setId(user.getId());
        vo.setPhone(mask(user.getPhone()));
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setRecyclerStatus(user.getRecyclerStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
