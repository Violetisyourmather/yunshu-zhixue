package com.yunshu.zhixue.modules.interview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.ai.dto.InterviewReportAi;
import com.yunshu.zhixue.modules.interview.dto.InterviewAnswerData;
import com.yunshu.zhixue.modules.interview.dto.InterviewTempSession;
import com.yunshu.zhixue.modules.interview.entity.InterviewQuestionScore;
import com.yunshu.zhixue.modules.interview.entity.InterviewRecord;
import com.yunshu.zhixue.modules.interview.entity.InterviewReport;
import com.yunshu.zhixue.modules.interview.mapper.InterviewQuestionScoreMapper;
import com.yunshu.zhixue.modules.interview.mapper.InterviewRecordMapper;
import com.yunshu.zhixue.modules.interview.mapper.InterviewReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试结束落库：记录、逐题评分与报告在同一事务中写入。
 */
@Service
public class InterviewPersistenceService {

    private final InterviewRecordMapper recordMapper;
    private final InterviewQuestionScoreMapper scoreMapper;
    private final InterviewReportMapper reportMapper;
    private final ObjectMapper objectMapper;

    public InterviewPersistenceService(InterviewRecordMapper recordMapper,
                                       InterviewQuestionScoreMapper scoreMapper,
                                       InterviewReportMapper reportMapper,
                                       ObjectMapper objectMapper) {
        this.recordMapper = recordMapper;
        this.scoreMapper = scoreMapper;
        this.reportMapper = reportMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public long persist(InterviewTempSession temp,
                        List<InterviewAnswerData> answers,
                        InterviewReportAi report,
                        int totalDuration,
                        LocalDateTime completedAt) {
        InterviewRecord record = new InterviewRecord();
        record.setStudentId(temp.getStudentId());
        record.setSubjectId(temp.getSubjectId());
        record.setDifficulty(temp.getDifficulty());
        record.setQuestionCount(temp.getQuestionCount());
        record.setOverallScore(decimal(report.overallScore()));
        record.setTotalDuration(totalDuration);
        record.setStartedAt(temp.getStartedAt());
        record.setCompletedAt(completedAt);
        recordMapper.insert(record);

        for (InterviewAnswerData answer : answers) {
            InterviewQuestionScore score = new InterviewQuestionScore();
            score.setInterviewRecordId(record.getId());
            score.setQuestionOrder(answer.order());
            score.setQuestionId(answer.questionId());
            score.setQuestionText(answer.questionText());
            score.setStudentAnswer(answer.studentAnswer());
            score.setScore(decimal(answer.score()));
            score.setAiFeedback(answer.feedback());
            scoreMapper.insert(score);
        }

        InterviewReport entity = new InterviewReport();
        entity.setInterviewRecordId(record.getId());
        try {
            entity.setRadarJson(objectMapper.writeValueAsString(report.dimensions()));
            entity.setStrengths(objectMapper.writeValueAsString(report.strengths()));
            entity.setWeaknesses(objectMapper.writeValueAsString(report.weaknesses()));
            entity.setSuggestions(objectMapper.writeValueAsString(report.suggestions()));
            entity.setImprovementCardsJson(objectMapper.writeValueAsString(report.improveCards()));
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "面试报告生成失败");
        }
        reportMapper.insert(entity);
        return entity.getId();
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}
