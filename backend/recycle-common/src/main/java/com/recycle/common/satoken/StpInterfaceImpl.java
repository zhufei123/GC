package com.recycle.common.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.common.entity.system.SysAdmin;
import com.recycle.common.entity.system.SysAdminRole;
import com.recycle.common.entity.system.SysMenu;
import com.recycle.common.entity.system.SysRole;
import com.recycle.common.entity.system.SysRoleMenu;
import com.recycle.common.mapper.SysAdminMapper;
import com.recycle.common.mapper.SysAdminRoleMapper;
import com.recycle.common.mapper.SysMenuMapper;
import com.recycle.common.mapper.SysRoleMapper;
import com.recycle.common.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 管理端角色/权限数据源；超级管理员返回 ["*:*:*"]
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysAdminMapper sysAdminMapper;
    private final SysAdminRoleMapper sysAdminRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (!StpKit.ADMIN_TYPE.equals(loginType)) {
            return List.of();
        }
        long adminId = Long.parseLong(String.valueOf(loginId));
        SysAdmin admin = sysAdminMapper.selectById(adminId);
        if (admin == null) {
            return List.of();
        }
        if (admin.getSuperAdmin() != null && admin.getSuperAdmin() == 1) {
            return List.of("*:*:*");
        }
        List<Long> roleIds = roleIdsOf(adminId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = sysRoleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, menuIds))
                .stream().map(SysMenu::getPerms)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (!StpKit.ADMIN_TYPE.equals(loginType)) {
            return List.of();
        }
        long adminId = Long.parseLong(String.valueOf(loginId));
        List<Long> roleIds = roleIdsOf(adminId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds))
                .stream().map(SysRole::getCode).toList();
    }

    private List<Long> roleIdsOf(long adminId) {
        return sysAdminRoleMapper.selectList(
                        new LambdaQueryWrapper<SysAdminRole>().eq(SysAdminRole::getAdminId, adminId))
                .stream().map(SysAdminRole::getRoleId).toList();
    }
}
