package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.admin.dto.MenuSaveDTO;
import com.recycle.admin.vo.MenuNodeVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.entity.system.SysMenu;
import com.recycle.common.entity.system.SysRoleMenu;
import com.recycle.common.mapper.SysMenuMapper;
import com.recycle.common.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /** 全量菜单树（含 BUTTON，供角色授权勾选） */
    public List<MenuNodeVO> tree() {
        List<SysMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
        return AdminAuthService.buildTree(menus);
    }

    public Long create(MenuSaveDTO dto) {
        SysMenu menu = new SysMenu();
        copy(dto, menu);
        menuMapper.insert(menu);
        return menu.getId();
    }

    public void update(Long id, MenuSaveDTO dto) {
        SysMenu menu = require(id);
        copy(dto, menu);
        menuMapper.updateById(menu);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (children > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "存在子菜单，不能删除");
        }
        menuMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
    }

    private SysMenu require(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        return menu;
    }

    private void copy(MenuSaveDTO dto, SysMenu menu) {
        menu.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        menu.setName(dto.getName());
        menu.setTitle(dto.getTitle());
        menu.setType(dto.getType());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setIcon(dto.getIcon());
        menu.setPerms(dto.getPerms());
        menu.setSort(dto.getSort() == null ? 0 : dto.getSort());
        menu.setVisible(dto.getVisible() == null ? 1 : dto.getVisible());
    }
}
