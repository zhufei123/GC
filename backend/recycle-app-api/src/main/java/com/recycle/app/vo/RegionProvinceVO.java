package com.recycle.app.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RegionProvinceVO {

    private String name;
    private List<RegionCityVO> cities = new ArrayList<>();
}
