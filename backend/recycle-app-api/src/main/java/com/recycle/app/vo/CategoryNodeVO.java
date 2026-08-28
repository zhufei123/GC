package com.recycle.app.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryNodeVO {

    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    private Integer status;
    private List<CategoryNodeVO> children = new ArrayList<>();
}
