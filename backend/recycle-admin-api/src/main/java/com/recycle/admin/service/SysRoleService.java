package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.admin.dto.RoleSaveDTO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.entity.system.SysAdminRole;
import com.recycle.common.entity.system.SysRole;
import com.recycle.common.entity.system.SysRoleMenu;
import com.recycle.common.mapper.SysAdminRoleMapper;
import com.recycle.common.mapper.SysRoleMapper;
import com.recycle.common.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysAdminRoleMapper adminRoleMapper;

    public List<SysRole> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    public Long create(RoleSaveDTO dto) {
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, dto.getCode()));
        if (exists > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        role.setRemark(dto.getRemark());
        roleMapper.insert(role);
        return role.getId();
    }

    public void update(Long id, RoleSaveDTO dto) {
        SysRole role = require(id);
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        role.setRemark(dto.getRemark());
        roleMapper.updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        Long used = adminRoleMapper.selectCount(new LambdaQueryWrapper<SysAdminRole>()
                .eq(SysAdminRole::getRoleId, id));
        if (used > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色已分配管理员，不能删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    public List<Long> menuIds(Long id) {
        require(id);
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, id))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    @Transactional
    public void assignMenus(Long id, List<Long> menuIds) {
        require(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                SysRoleMenu rel = new SysRoleMenu();
                rel.setRoleId(id);
                rel.setMenuId(menuId);
                roleMenuMapper.insert(rel);
            }
        }
    }

    private SysRole require(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }
}
