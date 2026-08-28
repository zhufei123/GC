package com.recycle.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点（前端动态路由使用 name/path/component/title/icon/children）
 */
@Data
public class MenuNodeVO {

    private Long id;
    private Long parentId;
    private String name;
    private String title;
    /** DIR/MENU/BUTTON */
    private String type;
    private String path;
    private String component;
    private String icon;
    private String perms;
    private Integer sort;
    private Integer visible;
    private List<MenuNodeVO> children = new ArrayList<>();
}
