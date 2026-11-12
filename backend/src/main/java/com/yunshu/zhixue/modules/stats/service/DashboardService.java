package com.yunshu.zhixue.modules.stats.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.stats.dto.DailyCountRow;
import com.yunshu.zhixue.modules.stats.dto.DashboardOverview;
import com.yunshu.zhixue.modules.stats.dto.QuestionDistributionItem;
import com.yunshu.zhixue.modules.stats.dto.RecentActivityItem;
import com.yunshu.zhixue.modules.stats.dto.UserGrowthPoint;
import com.yunshu.zhixue.modules.stats.mapper.DashboardMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端数据看板（A-D01 ~ A-D04）。
 *
 * <p>统计卡与分布均由明细表查询时聚合；概览结果按约定缓存 5 分钟，
 * 最近活动来自操作日志、要求实时，不缓存。所有自然日/自然月按 Asia/Shanghai 划分。</p>
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private static final Duration OVERVIEW_TTL = Duration.ofMinutes(5);
    private static final Set<Integer> GROWTH_RANGES = Set.of(7, 30, 90);
    private static final long MAX_PAGE_SIZE = 50;

    private final DashboardMapper dashboardMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public DashboardService(DashboardMapper dashboardMapper,
                            StringRedisTemplate redis,
                            ObjectMapper objectMapper) {
        this.dashboardMapper = dashboardMapper;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** A-D01 统计卡（含本自然月新增量），缓存 5 分钟。 */
    public DashboardOverview overview() {
        DashboardOverview cached = readOverviewCache();
        if (cached != null) {
            return cached;
        }
        LocalDateTime monthStart = monthStartUtc();
        DashboardOverview overview = new DashboardOverview(
                dashboardMapper.countSubjects(),
                dashboardMapper.countSubjectsSince(monthStart),
                dashboardMapper.countQuestions(),
                dashboardMapper.countQuestionsSince(monthStart),
                dashboardMapper.countStudents(),
                dashboardMapper.countStudentsSince(monthStart),
                dashboardMapper.countCompletedInterviews(),
                dashboardMapper.countCompletedInterviewsSince(monthStart));
        writeOverviewCache(overview);
        return overview;
    }

    private DashboardOverview readOverviewCache() {
        try {
            String cached = redis.opsForValue().get(RedisKeys.DASHBOARD_OVERVIEW);
            return cached == null ? null : objectMapper.readValue(cached, DashboardOverview.class);
        } catch (Exception ex) {
            log.warn("读取看板概览缓存失败，回源查询：{}", ex.getMessage());
            return null;
        }
    }

    private void writeOverviewCache(DashboardOverview overview) {
        try {
            redis.opsForValue().set(RedisKeys.DASHBOARD_OVERVIEW,
                    objectMapper.writeValueAsString(overview), OVERVIEW_TTL);
        } catch (Exception ex) {
            log.warn("写入看板概览缓存失败：{}", ex.getMessage());
        }
    }

    /** A-D02 用户增长曲线（日粒度，range ∈ 7/30/90，缺省 7）。 */
    public List<UserGrowthPoint> userGrowth(Integer range) {
        int days = range == null ? 7 : range;
        if (!GROWTH_RANGES.contains(days)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "时间范围仅支持 7 / 30 / 90 天");
        }
        LocalDate today = LocalDate.now(AppConstants.BUSINESS_ZONE);
        LocalDate start = today.minusDays(days - 1L);
        LocalDateTime startUtc = start.atStartOfDay(AppConstants.BUSINESS_ZONE)
                .toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endUtc = today.plusDays(1).atStartOfDay(AppConstants.BUSINESS_ZONE)
                .toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();

        Map<String, Long> counts = dashboardMapper.selectDailyRegistrations(startUtc, endUtc).stream()
                .collect(Collectors.toMap(
                        DailyCountRow::getDay,
                        row -> row.getTotal() == null ? 0L : row.getTotal(),
                        (first, ignored) -> first));

        List<UserGrowthPoint> points = new ArrayList<>(days);
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            points.add(new UserGrowthPoint(date.toString(), counts.getOrDefault(date.toString(), 0L)));
        }
        return points;
    }

    /** A-D03 题目学科分布（按题目数倒序）。 */
    public List<QuestionDistributionItem> questionDistribution() {
        return dashboardMapper.selectQuestionDistribution();
    }

    /** A-D04 最近活动（实时，不缓存）。 */
    public PageResult<RecentActivityItem> recentActivities(long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        long total = dashboardMapper.countActivities();
        if (total == 0) {
            return PageResult.empty(safePage, safePageSize);
        }
        List<RecentActivityItem> list = dashboardMapper.selectRecentActivities(
                safePageSize, (safePage - 1) * safePageSize);
        return PageResult.of(list, total, safePage, safePageSize);
    }

    /** 上海时区本自然月起点，转换为 UTC 存储时间。 */
    private LocalDateTime monthStartUtc() {
        return LocalDate.now(AppConstants.BUSINESS_ZONE)
                .withDayOfMonth(1)
                .atStartOfDay(AppConstants.BUSINESS_ZONE)
                .toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();
    }
}
