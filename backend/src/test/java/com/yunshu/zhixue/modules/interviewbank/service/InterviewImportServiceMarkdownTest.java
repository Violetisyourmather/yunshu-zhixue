package com.yunshu.zhixue.modules.interviewbank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownConfirmRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownPayload;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownPreview;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownQuestion;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownResult;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewQuestion;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewQuestionMapper;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewTopicMapper;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterviewImportServiceMarkdownTest {

    @Mock
    private InterviewQuestionMapper questionMapper;
    @Mock
    private InterviewTopicMapper topicMapper;
    @Mock
    private SubjectMapper subjectMapper;
    @Mock
    private InterviewQuestionService questionService;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private OperationLogRecorder operationLogRecorder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private InterviewImportService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        service = new InterviewImportService(questionMapper, topicMapper, subjectMapper,
                questionService, redis, objectMapper, operationLogRecorder);
    }

    @Test
    void previewParsesMarkdownHeadingsAndStoresToken() {
        String markdown = """
                # 第一题：什么是 RAG？

                RAG 是检索增强生成。

                ## 第二题：如何评估检索质量？

                离线看 Recall@K 与 NDCG。

                #### 四级标题不切题

                继续作为上一题的答案。
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "面试题.md", "text/markdown", markdown.getBytes(StandardCharsets.UTF_8));

        InterviewMarkdownPreview preview = service.previewMarkdown(file);

        assertThat(preview.questionCount()).isEqualTo(2);
        assertThat(preview.fileName()).isEqualTo("面试题.md");
        assertThat(preview.samples()).hasSize(2);
        assertThat(preview.samples().get(0).title()).isEqualTo("第一题：什么是 RAG？");
        assertThat(preview.samples().get(0).answerPreview()).contains("RAG 是检索增强生成");
        assertThat(preview.samples().get(1).title()).isEqualTo("第二题：如何评估检索质量？");
        assertThat(preview.token()).isNotBlank();
        verify(valueOperations).set(startsWith(RedisKeys.INTERVIEW_IMPORT_MARKDOWN),
                anyString(), any(Duration.class));
    }

    @Test
    void previewRejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "面试题.txt", "text/plain", "# Q\nA".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.previewMarkdown(file))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorCode())
                .isEqualTo(ErrorCode.IMPORT_INVALID);
    }

    @Test
    void previewRejectsMoreThan500Questions() {
        StringBuilder markdown = new StringBuilder();
        for (int index = 1; index <= 501; index++) {
            markdown.append("# 第 ").append(index).append(" 题\n\n答案 ").append(index).append("\n\n");
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.md", "text/markdown", markdown.toString().getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.previewMarkdown(file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("500");
    }

    @Test
    void confirmInsertsQuestionsWithDocumentSource() throws Exception {
        String token = "token-1";
        InterviewMarkdownPayload payload = new InterviewMarkdownPayload("面试题.md", List.of(
                new InterviewMarkdownQuestion(1, "第一题", "答案一", 1),
                new InterviewMarkdownQuestion(2, "第二题", "答案二", 4)));
        when(valueOperations.get(RedisKeys.INTERVIEW_IMPORT_MARKDOWN + token))
                .thenReturn(objectMapper.writeValueAsString(payload));
        when(subjectMapper.selectById(1L)).thenReturn(subject(1L));

        InterviewMarkdownResult result = service.confirmMarkdown(
                new InterviewMarkdownConfirmRequest(token, 1L, 2, "RAG", "检索,大模型"));

        assertThat(result.importedCount()).isEqualTo(2);
        verify(questionService).ensureTopic("RAG");
        verify(redis).delete(RedisKeys.INTERVIEW_IMPORT_MARKDOWN + token);
        verify(operationLogRecorder).record(anyString(), eq("面试题"), anyString());

        ArgumentCaptor<InterviewQuestion> captor = ArgumentCaptor.forClass(InterviewQuestion.class);
        verify(questionMapper, times(2)).insert(captor.capture());
        InterviewQuestion first = captor.getAllValues().get(0);
        assertThat(first.getSubjectId()).isEqualTo(1L);
        assertThat(first.getContent()).isEqualTo("第一题");
        assertThat(first.getReferenceAnswer()).isEqualTo("答案一");
        assertThat(first.getTopic()).isEqualTo("RAG");
        assertThat(first.getDifficulty()).isEqualTo(2);
        assertThat(first.getTags()).isEqualTo("检索,大模型");
        assertThat(first.getSourceType()).isEqualTo(3);
        assertThat(first.getSourceDocName()).isEqualTo("面试题.md");
        assertThat(first.getSourceSeq()).isEqualTo(1);
        assertThat(first.getStatus()).isEqualTo(1);
    }

    @Test
    void confirmRejectsExpiredToken() {
        when(valueOperations.get(RedisKeys.INTERVIEW_IMPORT_MARKDOWN + "missing")).thenReturn(null);

        assertThatThrownBy(() -> service.confirmMarkdown(
                new InterviewMarkdownConfirmRequest("missing", 1L, 2, null, null)))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(questionMapper, never()).insert(any(InterviewQuestion.class));
    }

    private Subject subject(long id) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName("测试方向");
        return subject;
    }
}
