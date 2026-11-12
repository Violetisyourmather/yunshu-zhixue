package com.yunshu.zhixue.modules.interview.controller;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.config.AsyncConfig;
import com.yunshu.zhixue.modules.ai.dto.InterviewAnswerGrade;
import com.yunshu.zhixue.modules.interview.dto.InterviewAnswerRequest;
import com.yunshu.zhixue.modules.interview.dto.InterviewConfigResponse;
import com.yunshu.zhixue.modules.interview.dto.InterviewFinishRequest;
import com.yunshu.zhixue.modules.interview.dto.InterviewFinishResponse;
import com.yunshu.zhixue.modules.interview.dto.InterviewRecordItem;
import com.yunshu.zhixue.modules.interview.dto.InterviewReportResponse;
import com.yunshu.zhixue.modules.interview.dto.InterviewStartRequest;
import com.yunshu.zhixue.modules.interview.dto.InterviewStartResponse;
import com.yunshu.zhixue.modules.interview.dto.InterviewStats;
import com.yunshu.zhixue.modules.interview.dto.InterviewTempSession;
import com.yunshu.zhixue.modules.interview.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学员端 AI 面试（04 §4.7 P-I01 ~ P-I05、P-I07 ~ P-I08）。
 */
@Tag(name = "学员 AI 面试", description = "P-I01 配置 / P-I02 发起 / P-I03 SSE 评分 / P-I04 结束 / P-I05 报告 / P-I07 记录 / P-I08 统计")
@RestController
@RequestMapping("/api/portal/v1/interview")
public class InterviewController {

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);
    private static final long SSE_TIMEOUT_MILLIS = 180_000L;

    private final InterviewService interviewService;
    private final ThreadPoolTaskExecutor interviewExecutor;

    public InterviewController(InterviewService interviewService,
                               @Qualifier(AsyncConfig.INTERVIEW_EXECUTOR)
                               ThreadPoolTaskExecutor interviewExecutor) {
        this.interviewService = interviewService;
        this.interviewExecutor = interviewExecutor;
    }

    @Operation(summary = "P-I01 面试配置")
    @GetMapping("/config")
    public ApiResponse<InterviewConfigResponse> config() {
        return ApiResponse.ok(interviewService.config());
    }

    @Operation(summary = "P-I02 发起面试")
    @PostMapping("/sessions")
    public ApiResponse<InterviewStartResponse> start(@Valid @RequestBody InterviewStartRequest request) {
        return ApiResponse.ok(interviewService.start(request));
    }

    /**
     * P-I03 单题评分（SSE）。
     *
     * <p>开流前同步校验会话与题号，会话失效会按普通 JSON 错误返回；
     * AI 评分与增量评语在独立线程池中推送。</p>
     */
    @Operation(summary = "P-I03 单题 AI 评分（SSE）")
    @PostMapping(value = "/sessions/{id}/answers", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter answer(@PathVariable String id,
                             @Valid @RequestBody InterviewAnswerRequest request) {
        InterviewTempSession session = interviewService.requireSession(id);
        interviewService.validateOrder(session, request.order());
        long studentId = session.getStudentId();

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        interviewExecutor.execute(() -> streamAnswer(emitter, id, request, studentId));
        return emitter;
    }

    private void streamAnswer(SseEmitter emitter, String sessionId, InterviewAnswerRequest request,
                              long studentId) {
        try {
            send(emitter, Map.of("phase", "analyzing"));
            InterviewAnswerGrade grade = interviewService.gradeAnswer(sessionId, request, studentId);
            send(emitter, Map.of("phase", "grading"));
            streamFeedback(emitter, grade.feedback());
            Map<String, Object> gradeEvent = new LinkedHashMap<>();
            gradeEvent.put("order", request.order());
            gradeEvent.put("score", grade.score());
            gradeEvent.put("feedback", grade.feedback());
            gradeEvent.put("degraded", grade.degraded());
            gradeEvent.put("referencePoints", grade.referencePoints());
            send(emitter, gradeEvent);
            // 末帧后追加一个心跳式 done 事件，确保上一帧被刷新到客户端
            send(emitter, Map.of("done", true));
            Thread.sleep(80L);
            emitter.complete();
        } catch (Exception ex) {
            int code = ex instanceof BizException biz ? biz.getErrorCode().code() : 20010;
            String message = ex instanceof BizException biz ? biz.getMessage() : "AI 评分失败，请稍后重试";
            log.warn("面试 SSE 评分失败：sessionId={}, order={}", sessionId, request.order(), ex);
            try {
                send(emitter, Map.of("code", code, "message", message));
                send(emitter, Map.of("done", true));
                Thread.sleep(80L);
            } catch (IOException | InterruptedException ignored) {
                // 客户端可能已断开
            }
            emitter.complete();
        }
    }

    private void streamFeedback(SseEmitter emitter, String feedback) throws IOException, InterruptedException {
        if (feedback == null || feedback.isEmpty()) {
            return;
        }
        int step = 12;
        for (int index = 0; index < feedback.length(); index += step) {
            String delta = feedback.substring(index, Math.min(feedback.length(), index + step));
            send(emitter, Map.of("delta", delta));
            Thread.sleep(18L);
        }
    }

    private void send(SseEmitter emitter, Object payload) throws IOException {
        emitter.send(SseEmitter.event().data(payload));
    }

    @Operation(summary = "P-I04 结束面试并生成报告")
    @PostMapping("/sessions/{id}/finish")
    public ApiResponse<InterviewFinishResponse> finish(@PathVariable String id,
                                                       @Valid @RequestBody InterviewFinishRequest request) {
        return ApiResponse.ok(interviewService.finish(id, request));
    }

    @Operation(summary = "P-I05 面试报告详情")
    @GetMapping("/reports/{id}")
    public ApiResponse<InterviewReportResponse> report(@PathVariable long id) {
        return ApiResponse.ok(interviewService.report(id));
    }

    @Operation(summary = "P-I07 面试记录")
    @GetMapping("/records")
    public ApiResponse<PageResult<InterviewRecordItem>> records(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(interviewService.records(page, pageSize));
    }

    @Operation(summary = "P-I08 面试统计")
    @GetMapping("/records/stats")
    public ApiResponse<InterviewStats> recordsStats() {
        return ApiResponse.ok(interviewService.recordsStats());
    }
}
