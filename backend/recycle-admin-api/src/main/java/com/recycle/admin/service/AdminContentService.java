package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.content.Banner;
import com.recycle.common.entity.content.Notice;
import com.recycle.common.mapper.BannerMapper;
import com.recycle.common.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminContentService {

    private final BannerMapper bannerMapper;
    private final NoticeMapper noticeMapper;

    public PageResult<Banner> bannerPage(Integer status, PageQuery query) {
        return PageResult.of(bannerMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Banner>()
                        .eq(status != null, Banner::getStatus, status)
                        .orderByAsc(Banner::getSort)
                        .orderByDesc(Banner::getId)));
    }

    public Long createBanner(Banner banner) {
        if (!StringUtils.hasText(banner.getImage())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "图片不能为空");
        }
        banner.setId(null);
        if (banner.getLinkType() == null) {
            banner.setLinkType("NONE");
        }
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        bannerMapper.insert(banner);
        return banner.getId();
    }

    public void updateBanner(Long id, Banner banner) {
        requireBanner(id);
        banner.setId(id);
        bannerMapper.updateById(banner);
    }

    public void deleteBanner(Long id) {
        requireBanner(id);
        bannerMapper.deleteById(id);
    }

    public void updateBannerStatus(Long id, Integer status) {
        Banner banner = requireBanner(id);
        banner.setStatus(status);
        bannerMapper.updateById(banner);
    }

    public PageResult<Notice> noticePage(String publishStatus, String title, PageQuery query) {
        return PageResult.of(noticeMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Notice>()
                        .eq(StringUtils.hasText(publishStatus), Notice::getPublishStatus, publishStatus)
                        .like(StringUtils.hasText(title), Notice::getTitle, title)
                        .orderByDesc(Notice::getPinned)
                        .orderByDesc(Notice::getId)));
    }

    public Long createNotice(Notice notice) {
        if (!StringUtils.hasText(notice.getTitle())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "标题不能为空");
        }
        notice.setId(null);
        if (notice.getPinned() == null) {
            notice.setPinned(0);
        }
        if (!StringUtils.hasText(notice.getPublishStatus())) {
            notice.setPublishStatus("published");
        }
        if ("published".equals(notice.getPublishStatus()) && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        noticeMapper.insert(notice);
        return notice.getId();
    }

    public void updateNotice(Long id, Notice notice) {
        requireNotice(id);
        notice.setId(id);
        if ("published".equals(notice.getPublishStatus()) && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        noticeMapper.updateById(notice);
    }

    public void deleteNotice(Long id) {
        requireNotice(id);
        noticeMapper.deleteById(id);
    }

    private Banner requireBanner(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Banner 不存在");
        }
        return banner;
    }

    private Notice requireNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        return notice;
    }
}
