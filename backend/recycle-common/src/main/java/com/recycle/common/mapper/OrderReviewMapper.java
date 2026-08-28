package com.recycle.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recycle.common.entity.trade.OrderReview;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

public interface OrderReviewMapper extends BaseMapper<OrderReview> {

    /** 门店评价统计：reviewCount + avgRating（无评价时 avgRating 为 null） */
    @Select("SELECT COUNT(*) AS reviewCount, AVG(rating) AS avgRating "
            + "FROM order_review WHERE station_id = #{stationId} AND deleted = 0")
    Map<String, Object> statsByStation(@Param("stationId") Long stationId);
}
