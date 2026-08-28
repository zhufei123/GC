package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.admin.dto.AdminLoginDTO;
import com.recycle.admin.vo.AdminLoginVO;
import com.recycle.admin.vo.MenuNodeVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.entity.system.SysAdmin;
import com.recycle.common.entity.system.SysAdminRole;
import com.recycle.common.entity.system.SysMenu;
import com.recycle.common.entity.system.SysRoleMenu;
import com.recycle.common.mapper.SysAdminMapper;
import com.recycle.common.mapper.SysAdminRoleMapper;
import com.recycle.common.mapper.SysMenuMapper;
import com.recycle.common.mapper.SysRoleMenuMapper;
import com.recycle.common.satoken.StpKit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final SysAdminMapper adminMapper;
    private final SysAdminRoleMapper adminRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final cn.dev33.satoken.stp.StpInterface stpInterface;

    public AdminLoginVO login(AdminLoginDTO dto) {
        SysAdmin admin = adminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUsername, dto.getUsername()));
        if (admin == null || !ENCODER.matches(dto.getPassword(), admin.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BizException(ErrorCode.USER_DISABLED, "账号已被禁用");
        }
        StpKit.ADMIN.login(admin.getId());
        return buildLoginVO(admin, StpKit.ADMIN.getTokenValue());
    }

    public void logout() {
        if (StpKit.ADMIN.isLogin()) {
            StpKit.ADMIN.logout();
        }
    }

    public AdminLoginVO me(Long adminId) {
        SysAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return buildLoginVO(admin, null);
    }

    /** 动态路由菜单树（DIR/MENU，超管返回全部） */
    public List<MenuNodeVO> menus(Long adminId) {
        SysAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        List<SysMenu> menus;
        if (admin.getSuperAdmin() != null && admin.getSuperAdmin() == 1) {
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getType, List.of("DIR", "MENU"))
                    .orderByAsc(SysMenu::getSort));
        } else {
            List<Long> roleIds = adminRoleMapper.selectList(new LambdaQueryWrapper<SysAdminRole>()
                            .eq(SysAdminRole::getAdminId, adminId))
                    .stream().map(SysAdminRole::getRoleId).toList();
            if (roleIds.isEmpty()) {
                return List.of();
            }
            List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                            .in(SysRoleMenu::getRoleId, roleIds))
                    .stream().map(SysRoleMenu::getMenuId).distinct().toList();
            if (menuIds.isEmpty()) {
                return List.of();
            }
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getId, menuIds)
                    .in(SysMenu::getType, List.of("DIR", "MENU"))
                    .orderByAsc(SysMenu::getSort));
        }
        return buildTree(menus);
    }

    public static List<MenuNodeVO> buildTree(List<SysMenu> menus) {
        Map<Long, MenuNodeVO> nodes = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            MenuNodeVO node = new MenuNodeVO();
            node.setId(menu.getId());
            node.setParentId(menu.getParentId());
            node.setName(menu.getName());
            node.setTitle(menu.getTitle());
            node.setType(menu.getType());
            node.setPath(menu.getPath());
            node.setComponent(menu.getComponent());
            node.setIcon(menu.getIcon());
            node.setPerms(menu.getPerms());
            node.setSort(menu.getSort());
            node.setVisible(menu.getVisible());
            nodes.put(menu.getId(), node);
        }
        List<MenuNodeVO> roots = new ArrayList<>();
        for (MenuNodeVO node : nodes.values()) {
            MenuNodeVO parent = nodes.get(node.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        Comparator<MenuNodeVO> bySort = Comparator.comparing(MenuNodeVO::getSort,
                Comparator.nullsLast(Comparator.naturalOrder()));
        nodes.values().forEach(n -> n.getChildren().sort(bySort));
        roots.sort(bySort);
        return roots;
    }

    private AdminLoginVO buildLoginVO(SysAdmin admin, String token) {
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        AdminLoginVO.AdminProfileVO profile = new AdminLoginVO.AdminProfileVO();
        profile.setId(admin.getId());
        profile.setUsername(admin.getUsername());
        profile.setNickname(admin.getNickname());
        profile.setAvatar(admin.getAvatar() == null ? "" : admin.getAvatar());
        vo.setAdmin(profile);
        vo.setRoles(stpInterface.getRoleList(admin.getId(), StpKit.ADMIN_TYPE));
        vo.setPerms(stpInterface.getPermissionList(admin.getId(), StpKit.ADMIN_TYPE));
        return vo;
    }
}
