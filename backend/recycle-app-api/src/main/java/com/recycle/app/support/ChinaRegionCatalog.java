package com.recycle.app.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recycle.app.vo.RegionCityVO;
import com.recycle.app.vo.RegionProvinceVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 全国省-市二级区划（地级市，含直辖市/港澳台），坐标为城市中心点。
 */
@Slf4j
@Component
public class ChinaRegionCatalog {

    private List<RegionProvinceVO> provinces = List.of();

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource("geo/china-regions.json").getInputStream()) {
            List<Map<String, Object>> raw = new ObjectMapper().readValue(in, new TypeReference<>() { });
            List<RegionProvinceVO> list = new ArrayList<>();
            for (Map<String, Object> p : raw) {
                RegionProvinceVO vo = new RegionProvinceVO();
                vo.setName(String.valueOf(p.get("name")));
                Object cities = p.get("cities");
                if (cities instanceof List<?> cityList) {
                    for (Object c : cityList) {
                        if (!(c instanceof Map<?, ?> cm)) {
                            continue;
                        }
                        RegionCityVO city = new RegionCityVO();
                        city.setName(String.valueOf(cm.get("name")));
                        city.setLongitude(decimal(cm.get("longitude")));
                        city.setLatitude(decimal(cm.get("latitude")));
                        vo.getCities().add(city);
                    }
                }
                list.add(vo);
            }
            this.provinces = List.copyOf(list);
            log.info("[geo] loaded {} provinces / {} cities",
                    provinces.size(),
                    provinces.stream().mapToInt(x -> x.getCities().size()).sum());
        } catch (Exception e) {
            log.error("[geo] failed to load china-regions.json", e);
            this.provinces = List.of();
        }
    }

    public List<RegionProvinceVO> provinces() {
        return provinces;
    }

    private static BigDecimal decimal(Object v) {
        if (v == null) {
            return null;
        }
        return new BigDecimal(v.toString());
    }
}
