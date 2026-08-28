package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.app.vo.HomeVO;
import com.recycle.app.vo.StoreNearbyVO;
import com.recycle.app.vo.TimeslotVO;
import com.recycle.common.entity.content.Banner;
import com.recycle.common.entity.content.Notice;
import com.recycle.common.entity.recycle.Category;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.mapper.BannerMapper;
import com.recycle.common.mapper.CategoryMapper;
import com.recycle.common.mapper.NoticeMapper;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.util.GeoUtils;
import com.recycle.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppHomeService {

    private static final List<String> PERIODS =
            List.of("09:00-11:00", "11:00-13:00", "14:00-16:00", "16:00-18:00");
    private static final BigDecimal NEARBY_RADIUS_KM = BigDecimal.valueOf(10);

    private final BannerMapper bannerMapper;
    private final CategoryMapper categoryMapper;
    private final NoticeMapper noticeMapper;
    private final RecycleStationMapper stationMapper;

    public HomeVO home() {
        LocalDateTime now = LocalDateTime.now();
        HomeVO vo = new HomeVO();
        vo.setBanners(bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getStatus, 1)
                        .and(w -> w.isNull(Banner::getStartTime).or().le(Banner::getStartTime, now))
                        .and(w -> w.isNull(Banner::getEndTime).or().ge(Banner::getEndTime, now))
                        .orderByAsc(Banner::getSort))
                .stream().map(b -> {
                    HomeVO.BannerVO item = new HomeVO.BannerVO();
                    item.setId(b.getId());
                    item.setTitle(b.getTitle());
                    item.setImage(b.getImage());
                    item.setLinkType(b.getLinkType());
                    item.setLinkUrl(b.getLinkUrl());
                    return item;
                }).toList());
        vo.setHotCategories(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, 0L)
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
                        .last("LIMIT 8"))
                .stream().map(c -> {
                    HomeVO.HotCategoryVO item = new HomeVO.HotCategoryVO();
                    item.setId(c.getId());
                    item.setName(c.getName());
                    item.setIcon(c.getIcon());
                    return item;
                }).toList());
        vo.setNotices(noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getPublishStatus, "published")
                        .orderByDesc(Notice::getPinned)
                        .orderByDesc(Notice::getPublishTime)
                        .last("LIMIT 3"))
                .stream().map(n -> {
                    HomeVO.NoticeVO item = new HomeVO.NoticeVO();
                    item.setId(n.getId());
                    item.setTitle(n.getTitle());
                    item.setPinned(n.getPinned());
                    item.setPublishTime(n.getPublishTime());
                    return item;
                }).toList());
        return vo;
    }

    public List<TimeslotVO> timeslots() {
        List<TimeslotVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 3; i++) {
            TimeslotVO vo = new TimeslotVO();
            LocalDate date = today.plusDays(i);
            vo.setDate(date);
            vo.setDateLabel(i == 0 ? "今天" : i == 1 ? "明天" : "后天");
            vo.setPeriods(PERIODS);
            result.add(vo);
        }
        return result;
    }

    public List<StoreNearbyVO> nearby(BigDecimal longitude, BigDecimal latitude) {
        List<RecycleStation> stations = stationMapper.selectList(new LambdaQueryWrapper<RecycleStation>()
                .eq(RecycleStation::getAuditStatus, "approved")
                .eq(RecycleStation::getStatus, 1));
        return stations.stream()
                .map(s -> {
                    StoreNearbyVO vo = new StoreNearbyVO();
                    vo.setId(s.getId());
                    vo.setName(s.getName());
                    vo.setAddress(s.getAddress());
                    vo.setPhone(s.getPhone());
                    vo.setBusinessHours(s.getBusinessHours());
                    vo.setBusinessStatus(s.getBusinessStatus());
                    vo.setLongitude(s.getLongitude());
                    vo.setLatitude(s.getLatitude());
                    vo.setCategoryIds(JsonUtils.toLongList(s.getCategoryIds()));
                    vo.setPhotos(JsonUtils.toStringList(s.getPhotos()));
                    vo.setDistanceKm(GeoUtils.distanceKm(longitude, latitude, s.getLongitude(), s.getLatitude()));
                    return vo;
                })
                .filter(vo -> vo.getDistanceKm() == null || vo.getDistanceKm().compareTo(NEARBY_RADIUS_KM) <= 0)
                .sorted(Comparator.comparing(StoreNearbyVO::getDistanceKm,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
