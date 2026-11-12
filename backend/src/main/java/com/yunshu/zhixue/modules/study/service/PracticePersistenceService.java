package com.yunshu.zhixue.modules.study.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.modules.study.dto.PracticeGradedAnswer;
import com.yunshu.zhixue.modules.study.entity.PracticeRecord;
import com.yunshu.zhixue.modules.study.entity.WrongQuestion;
import com.yunshu.zhixue.modules.study.mapper.PracticeRecordMapper;
import com.yunshu.zhixue.modules.study.mapper.WrongQuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 刷题作答落库与错题本维护。
 *
 * <p>统计以作答明细为事实来源；错题连续答对 3 次转已掌握，
 * 已掌握后再次答错回退待复习并将纠正次数归零。</p>
 */
@Service
public class PracticePersistenceService {

    private final PracticeRecordMapper practiceRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;

    public PracticePersistenceService(PracticeRecordMapper practiceRecordMapper,
                                      WrongQuestionMapper wrongQuestionMapper) {
        this.practiceRecordMapper = practiceRecordMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
    }

    @Transactional
    public void persist(long studentId, int mode, List<PracticeGradedAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (PracticeGradedAnswer answer : answers) {
            PracticeRecord record = new PracticeRecord();
            record.setStudentId(studentId);
            record.setQuestionId(answer.questionId());
            record.setMode(mode);
            record.setIsCorrect(answer.correct() ? 1 : 0);
            record.setDurationSeconds(answer.durationSeconds());
            record.setPracticedAt(now);
            practiceRecordMapper.insert(record);

            if (answer.correct()) {
                markCorrect(studentId, answer.questionId());
            } else {
                markWrong(studentId, answer.questionId(), answer.studentAnswer(), now);
            }
        }
    }

    private void markWrong(long studentId, long questionId, String studentAnswer, LocalDateTime now) {
        WrongQuestion active = findActive(studentId, questionId);
        if (active != null) {
            active.setErrorCount(nullSafe(active.getErrorCount()) + 1);
            active.setCorrectedCount(0);
            active.setStatus(1);
            active.setLastWrongAt(now);
            active.setLastAnswer(studentAnswer);
            wrongQuestionMapper.updateById(active);
            return;
        }

        WrongQuestion deleted = wrongQuestionMapper.findIncludingDeleted(studentId, questionId);
        if (deleted != null) {
            wrongQuestionMapper.restore(deleted.getId(), now, studentAnswer);
            return;
        }

        WrongQuestion wrong = new WrongQuestion();
        wrong.setStudentId(studentId);
        wrong.setQuestionId(questionId);
        wrong.setErrorCount(1);
        wrong.setCorrectedCount(0);
        wrong.setStatus(1);
        wrong.setLastWrongAt(now);
        wrong.setLastAnswer(studentAnswer);
        wrongQuestionMapper.insert(wrong);
    }

    private void markCorrect(long studentId, long questionId) {
        WrongQuestion active = findActive(studentId, questionId);
        if (active == null || nullSafe(active.getStatus()) == 2) {
            return;
        }
        int corrected = nullSafe(active.getCorrectedCount()) + 1;
        active.setCorrectedCount(corrected);
        if (corrected >= AppConstants.WRONG_QUESTION_MASTER_THRESHOLD) {
            active.setStatus(2);
        }
        wrongQuestionMapper.updateById(active);
    }

    private WrongQuestion findActive(long studentId, long questionId) {
        return wrongQuestionMapper.selectOne(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getStudentId, studentId)
                .eq(WrongQuestion::getQuestionId, questionId));
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
