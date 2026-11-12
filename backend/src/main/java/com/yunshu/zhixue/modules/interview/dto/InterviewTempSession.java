package com.yunshu.zhixue.modules.interview.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis 面试会话暂存（interview:temp:{sessionId}，TTL 24h）。
 */
@Data
public class InterviewTempSession {

    private String sessionId;

    private long studentId;

    private long subjectId;

    private String subjectName;

    private int difficulty;

    private int questionCount;

    private LocalDateTime startedAt;

    private List<InterviewTempQuestion> questions = new ArrayList<>();

    private Map<Integer, InterviewTempGrade> grades = new LinkedHashMap<>();
}
