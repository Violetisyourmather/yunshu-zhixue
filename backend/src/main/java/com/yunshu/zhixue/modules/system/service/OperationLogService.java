package com.yunshu.zhixue.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.LogExport;
import com.yunshu.zhixue.modules.system.dto.OperationLogItem;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import com.yunshu.zhixue.modules.system.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * 操作日志（A-G01 ~ A-G04）。
 *
 * <p>筛选日期按上海自然日解释后转 UTC 查询；导出上限 1 万行（04 §6 导出量级限制）；清空为物理删除。</p>
 */
@Service
public class OperationLogService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPORT_MAX_ROWS = 10_000;

    private static final DateTimeFormatter FILE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> ACTIONS = Set.of(
            OperationActions.LOGIN, OperationActions.LOGOUT, OperationActions.CREATE,
            OperationActions.UPDATE, OperationActions.DELETE, OperationActions.STATUS,
            OperationActions.EXPORT, OperationActions.OTHER);

    private final OperationLogMapper operationLogMapper;
    private final OperationLogRecorder operationLogRecorder;

    public OperationLogService(OperationLogMapper operationLogMapper,
                               OperationLogRecorder operationLogRecorder) {
        this.operationLogMapper = operationLogMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** A-G01 列表。 */
    public PageResult<OperationLogItem> list(String keyword, String operationType,
                                             String startDate, String endDate,
                                             long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        Page<OperationLog> result = operationLogMapper.selectPage(
                new Page<>(safePage, safePageSize),
                buildQuery(keyword, operationType, startDate, endDate));
        List<OperationLogItem> items = result.getRecords().stream().map(this::toItem).toList();
        return PageResult.of(items, result.getTotal(), safePage, safePageSize);
    }

    /** A-G02 详情。 */
    public OperationLogItem detail(Long id) {
        OperationLog entry = operationLogMapper.selectById(id);
        if (entry == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "日志记录不存在");
        }
        return toItem(entry);
    }

    /** A-G03 导出 CSV（带 BOM，Excel 直接打开不乱码）。 */
    public LogExport export(String keyword, String operationType, String startDate, String endDate) {
        var query = buildQuery(keyword, operationType, startDate, endDate)
                .last("LIMIT " + (EXPORT_MAX_ROWS + 1));
        List<OperationLog> rows = operationLogMapper.selectList(query);
        if (rows.size() > EXPORT_MAX_ROWS) {
            throw new BizException(ErrorCode.PARAM_INVALID, "导出数据超过 1 万行，请缩小筛选范围");
        }

        StringBuilder csv = new StringBuilder();
        csv.append((char) 0xFEFF);
        csv.append("日志ID,操作用户,操作类型,操作对象,IP地址,操作时间\r\n");
        for (OperationLog row : rows) {
            csv.append(row.getId()).append(',')
                    .append(csvCell(row.getUsername())).append(',')
                    .append(csvCell(row.getAction())).append(',')
                    .append(csvCell(row.getTargetType() + " - " + row.getTargetName())).append(',')
                    .append(csvCell(row.getIp())).append(',')
                    .append(csvCell(formatTime(row.getCreatedAt()))).append("\r\n");
        }

        String filename = "操作日志_" + LocalDateTime.now(AppConstants.BUSINESS_ZONE)
                .format(FILE_TIME_FORMATTER) + ".csv";
        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        operationLogRecorder.record(OperationActions.EXPORT, "操作日志", "导出 " + rows.size() + " 条日志");
        return new LogExport(content, filename);
    }

    /** A-G04 清空日志（物理删除），并留下一条清空记录。 */
    public int clear() {
        int deleted = operationLogMapper.deleteAll();
        operationLogRecorder.record(OperationActions.DELETE, "操作日志", "清空全部日志");
        return deleted;
    }

    private LambdaQueryWrapper<OperationLog> buildQuery(String keyword, String operationType,
                                                        String startDate, String endDate) {
        var query = Wrappers.<OperationLog>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(w -> w.like(OperationLog::getUsername, value)
                    .or().like(OperationLog::getAction, value)
                    .or().like(OperationLog::getTargetName, value));
        }
        if (StringUtils.hasText(operationType)) {
            String action = operationType.trim();
            if (!ACTIONS.contains(action)) {
                throw new BizException(ErrorCode.PARAM_INVALID, "操作类型不合法");
            }
            query.eq(OperationLog::getAction, action);
        }
        LocalDate start = parseDate(startDate, "开始日期");
        LocalDate end = parseDate(endDate, "结束日期");
        if (start != null && end != null && start.isAfter(end)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "开始日期不能晚于结束日期");
        }
        if (start != null) {
            query.ge(OperationLog::getCreatedAt, toUtcStart(start));
        }
        if (end != null) {
            query.lt(OperationLog::getCreatedAt, toUtcStart(end.plusDays(1)));
        }
        query.orderByDesc(OperationLog::getId);
        return query;
    }

    private LocalDate parseDate(String raw, String fieldName) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, fieldName + "格式应为 yyyy-MM-dd");
        }
    }

    private LocalDateTime toUtcStart(LocalDate date) {
        return date.atStartOfDay(AppConstants.BUSINESS_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    private String formatTime(LocalDateTime utcTime) {
        if (utcTime == null) {
            return "";
        }
        return utcTime.toInstant(ZoneOffset.UTC)
                .atZone(AppConstants.BUSINESS_ZONE)
                .format(DISPLAY_FORMATTER);
    }

    private OperationLogItem toItem(OperationLog entry) {
        return new OperationLogItem(entry.getId(), entry.getUserId(), entry.getUsername(), entry.getAction(),
                entry.getTargetType(), entry.getTargetName(), entry.getIp(), entry.getCreatedAt());
    }

    private static String csvCell(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
