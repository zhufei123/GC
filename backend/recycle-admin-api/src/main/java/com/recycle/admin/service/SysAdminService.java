package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recycle.admin.dto.AdminSaveDTO;
import com.recycle.admin.vo.AdminInfoVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.system.SysAdmin;
import com.recycle.common.entity.system.SysAdminRole;
import com.recycle.common.entity.system.SysRole;
import com.recycle.common.mapper.SysAdminMapper;
import com.recycle.common.mapper.SysAdminRoleMapper;
import com.recycle.common.mapper.SysRoleMapper;
import com.recycle.common.satoken.StpKit;
import com.recycle.common.util.QueryParams;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysAdminService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final SysAdminMapper adminMapper;
    private final SysAdminRoleMapper adminRoleMapper;
    private final SysRoleMapper roleMapper;

    public PageResult<AdminInfoVO> page(String username, Integer status, PageQuery query) {
        String keyword = QueryParams.firstText(query.getKeyword(), username);
        Page<SysAdmin> page = adminMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<SysAdmin>()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(SysAdmin::getUsername, keyword)
                                .or()
                                .like(SysAdmin::getNickname, keyword)
                                .or()
                                .like(SysAdmin::getPhone, keyword))
                        .eq(status != null, SysAdmin::getStatus, status)
                        .orderByAsc(SysAdmin::getId));
        List<Long> adminIds = page.getRecords().stream().map(SysAdmin::getId).toList();
        Map<Long, List<Long>> roleIdMap = adminIds.isEmpty() ? Map.of()
                : adminRoleMapper.selectList(new LambdaQueryWrapper<SysAdminRole>()
                        .in(SysAdminRole::getAdminId, adminIds))
                .stream().collect(Collectors.groupingBy(SysAdminRole::getAdminId,
                        Collectors.mapping(SysAdminRole::getRoleId, Collectors.toList())));
        Map<Long, String> roleNames = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getName));
        return PageResult.of(page, admin -> {
            AdminInfoVO vo = new AdminInfoVO();
            vo.setId(admin.getId());
            vo.setUsername(admin.getUsername());
            vo.setNickname(admin.getNickname());
            vo.setPhone(admin.getPhone());
            vo.setAvatar(admin.getAvatar());
            vo.setStatus(admin.getStatus());
            vo.setSuperAdmin(admin.getSuperAdmin());
            vo.setCreateTime(admin.getCreateTime());
            List<Long> roleIds = roleIdMap.getOrDefault(admin.getId(), List.of());
            vo.setRoleIds(roleIds);
            vo.setRoleNames(roleIds.stream().map(roleNames::get).filter(n -> n != null).toList());
            return vo;
        });
    }

    @Transactional
    public Long create(AdminSaveDTO dto) {
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        Long exists = adminMapper.selectCount(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUsername, dto.getUsername()));
        if (exists > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }
        SysAdmin admin = new SysAdmin();
        admin.setUsername(dto.getUsername());
        admin.setPassword(ENCODER.encode(dto.getPassword()));
        admin.setNickname(dto.getNickname());
        admin.setPhone(dto.getPhone());
        admin.setAvatar(dto.getAvatar());
        admin.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        admin.setSuperAdmin(0);
        adminMapper.insert(admin);
        saveRoles(admin.getId(), dto.getRoleIds());
        return admin.getId();
    }

    @Transactional
    public void update(Long id, AdminSaveDTO dto) {
        SysAdmin admin = require(id);
        admin.setNickname(dto.getNickname());
        admin.setPhone(dto.getPhone());
        admin.setAvatar(dto.getAvatar());
        if (dto.getStatus() != null) {
            admin.setStatus(dto.getStatus());
        }
        adminMapper.updateById(admin);
        if (dto.getRoleIds() != null) {
            adminRoleMapper.delete(new LambdaQueryWrapper<SysAdminRole>()
                    .eq(SysAdminRole::getAdminId, id));
            saveRoles(id, dto.getRoleIds());
        }
    }

    public void updateStatus(Long id, Integer status) {
        SysAdmin admin = require(id);
        admin.setStatus(status);
        adminMapper.updateById(admin);
        if (status != null && status == 0) {
            try {
                StpKit.ADMIN.kickout(id);
            } catch (Exception ignored) {
                // 未在线时忽略
            }
        }
    }

    public void resetPassword(Long id, String password) {
        SysAdmin admin = require(id);
        admin.setPassword(ENCODER.encode(password));
        adminMapper.updateById(admin);
    }

    @Transactional
    public void delete(Long id) {
        SysAdmin admin = require(id);
        if (admin.getSuperAdmin() != null && admin.getSuperAdmin() == 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能删除超级管理员");
        }
        adminMapper.deleteById(id);
        adminRoleMapper.delete(new LambdaQueryWrapper<SysAdminRole>()
                .eq(SysAdminRole::getAdminId, id));
    }

    private SysAdmin require(Long id) {
        SysAdmin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "管理员不存在");
        }
        return admin;
    }

    private void saveRoles(Long adminId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            SysAdminRole rel = new SysAdminRole();
            rel.setAdminId(adminId);
            rel.setRoleId(roleId);
            adminRoleMapper.insert(rel);
        }
    }
}
