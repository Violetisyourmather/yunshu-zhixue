package com.yunshu.zhixue.modules.stats.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.stats.dto.DailyCountRow;
import com.yunshu.zhixue.modules.stats.dto.DashboardOverview;
import com.yunshu.zhixue.modules.stats.dto.RecentActivityItem;
import com.yunshu.zhixue.modules.stats.dto.UserGrowthPoint;
import com.yunshu.zhixue.modules.stats.mapper.DashboardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock
    private DashboardMapper dashboardMapper;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DashboardService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        service = new DashboardService(dashboardMapper, redis, objectMapper);
    }

    @Test
    void overviewReadsFromCacheWhenPresent() throws Exception {
        DashboardOverview cached = new DashboardOverview(9, 1, 100, 20, 3, 1, 5, 2);
        when(valueOperations.get(RedisKeys.DASHBOARD_OVERVIEW))
                .thenReturn(objectMapper.writeValueAsString(cached));

        DashboardOverview result = service.overview();

        assertThat(result).isEqualTo(cached);
        verify(dashboardMapper, never()).countSubjects();
    }

    @Test
    void overviewFallsBackToDatabaseAndCachesWhenCacheMisses() {
        when(valueOperations.get(RedisKeys.DASHBOARD_OVERVIEW)).thenReturn(null);
        when(dashboardMapper.countSubjects()).thenReturn(9L);
        when(dashboardMapper.countSubjectsSince(any())).thenReturn(1L);
        when(dashboardMapper.countQuestions()).thenReturn(313L);
        when(dashboardMapper.countQuestionsSince(any())).thenReturn(46L);
        when(dashboardMapper.countStudents()).thenReturn(2L);
        when(dashboardMapper.countStudentsSince(any())).thenReturn(1L);
        when(dashboardMapper.countCompletedInterviews()).thenReturn(5L);
        when(dashboardMapper.countCompletedInterviewsSince(any())).thenReturn(2L);

        DashboardOverview result = service.overview();

        assertThat(result.subjectCount()).isEqualTo(9);
        assertThat(result.questionDelta()).isEqualTo(46);
        assertThat(result.studentDelta()).isEqualTo(1);
        assertThat(result.interviewDelta()).isEqualTo(2);
        verify(valueOperations).set(eq(RedisKeys.DASHBOARD_OVERVIEW), anyString(), any());
    }

    @Test
    void userGrowthFillsMissingDaysWithZero() {
        LocalDate today = LocalDate.now(AppConstants.BUSINESS_ZONE);
        DailyCountRow todayRow = row(today.toString(), 5);
        DailyCountRow yesterdayRow = row(today.minusDays(1).toString(), 2);
        when(dashboardMapper.selectDailyRegistrations(any(), any()))
                .thenReturn(List.of(yesterdayRow, todayRow));

        List<UserGrowthPoint> points = service.userGrowth(7);

        assertThat(points).hasSize(7);
        assertThat(points.get(6).date()).isEqualTo(today.toString());
        assertThat(points.get(6).count()).isEqualTo(5);
        assertThat(points.get(5).count()).isEqualTo(2);
        assertThat(points.get(0).count()).isZero();
    }

    @Test
    void userGrowthRejectsUnsupportedRange() {
        assertThatThrownBy(() -> service.userGrowth(14))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    void recentActivitiesReturnsEmptyPageWhenNoLogs() {
        when(dashboardMapper.countActivities()).thenReturn(0L);

        PageResult<RecentActivityItem> result = service.recentActivities(1, 6);

        assertThat(result.list()).isEmpty();
        assertThat(result.total()).isZero();
        verify(dashboardMapper, never()).selectRecentActivities(any(Long.class), any(Long.class));
    }

    @Test
    void recentActivitiesClampsPageSizeAndComputesOffset() {
        RecentActivityItem item = new RecentActivityItem(1L, "admin", "新增", "题库",
                "题目 1", "127.0.0.1", LocalDateTime.now());
        when(dashboardMapper.countActivities()).thenReturn(3L);
        when(dashboardMapper.selectRecentActivities(50L, 100L)).thenReturn(List.of(item));

        PageResult<RecentActivityItem> result = service.recentActivities(3, 100);

        assertThat(result.pageSize()).isEqualTo(50);
        assertThat(result.list()).containsExactly(item);
    }

    private DailyCountRow row(String day, long total) {
        DailyCountRow row = new DailyCountRow();
        row.setDay(day);
        row.setTotal(total);
        return row;
    }
}
