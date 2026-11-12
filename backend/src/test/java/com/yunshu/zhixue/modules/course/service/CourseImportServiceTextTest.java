package com.yunshu.zhixue.modules.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.modules.course.dto.CourseImportPreview;
import com.yunshu.zhixue.modules.course.dto.CourseImportRow;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.course.mapper.CourseSubjectMapper;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseImportServiceTextTest {

    @Mock
    private CourseMapper courseMapper;
    @Mock
    private CourseSubjectMapper courseSubjectMapper;
    @Mock
    private SubjectMapper subjectMapper;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private OperationLogRecorder operationLogRecorder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CourseImportService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(subjectMapper.selectList(any())).thenReturn(List.of(subject(1L, "Java后端开发")));
        when(courseMapper.selectList(any())).thenReturn(List.of());
        service = new CourseImportService(courseMapper, courseSubjectMapper, subjectMapper,
                redis, objectMapper, operationLogRecorder);
    }

    @Test
    void previewTextParsesHeaderAndRow() {
        String text = """
                课程名称 | 所属学科 | 来源 | 时长（分钟） | BV号 | 封面 | 播放链接
                Java 基础 | Java后端开发 | 其他 | 120 | | | https://example.com/java
                """;

        CourseImportPreview preview = service.previewText(text);

        assertThat(preview.totalRows()).isEqualTo(1);
        assertThat(preview.validCount()).isEqualTo(1);
        assertThat(preview.warningCount()).isZero();
        assertThat(preview.invalidCount()).isZero();
        assertThat(preview.token()).isNotBlank();

        CourseImportRow row = preview.rows().get(0);
        assertThat(row.name()).isEqualTo("Java 基础");
        assertThat(row.durationMinutes()).isEqualTo(120);
        assertThat(row.subjectId()).isEqualTo(1L);
        assertThat(row.source()).isEqualTo(2);
        assertThat(row.link()).isEqualTo("https://example.com/java");
        assertThat(row.status()).isEqualTo("valid");

        verify(valueOperations).set(startsWith(RedisKeys.COURSE_IMPORT_PREVIEW), anyString(),
                any(Duration.class));
    }

    @Test
    void previewTextWithoutHeaderUsesRequirementColumnOrder() {
        String text = "MySQL 入门 | 2:30 | Java后端开发 | 其他 | https://example.com/course |";

        CourseImportPreview preview = service.previewText(text);

        assertThat(preview.totalRows()).isEqualTo(1);
        assertThat(preview.validCount()).as("%s", preview.rows().get(0)).isEqualTo(1);
        CourseImportRow row = preview.rows().get(0);
        assertThat(row.name()).isEqualTo("MySQL 入门");
        assertThat(row.durationMinutes()).isEqualTo(150);
        assertThat(row.subjectId()).isEqualTo(1L);
        assertThat(row.source()).isEqualTo(2);
        assertThat(row.link()).isEqualTo("https://example.com/course");
    }

    @Test
    void previewTextWithoutHeaderAlsoAcceptsExcelColumnOrder() {
        String text = "Excel 课程 | Java后端开发 | 其他 | 90 | | | https://example.com/excel";

        CourseImportPreview preview = service.previewText(text);

        assertThat(preview.validCount()).isEqualTo(1);
        CourseImportRow row = preview.rows().get(0);
        assertThat(row.name()).isEqualTo("Excel 课程");
        assertThat(row.durationMinutes()).isEqualTo(90);
        assertThat(row.subjectId()).isEqualTo(1L);
        assertThat(row.source()).isEqualTo(2);
        assertThat(row.link()).isEqualTo("https://example.com/excel");
    }

    @Test
    void previewTextIgnoresBlankAndCommentLines() {
        String text = """
                # 这是注释
                Java 基础 | 60 | Java后端开发 | 其他 | https://example.com/java |
                // 这也是注释
                """;

        CourseImportPreview preview = service.previewText(text);

        assertThat(preview.totalRows()).isEqualTo(1);
        assertThat(preview.rows().get(0).name()).isEqualTo("Java 基础");
    }

    @Test
    void previewTextRejectsBlankInput() {
        assertThatThrownBy(() -> service.previewText("  \n  "))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("请粘贴课程文本");
    }

    @Test
    void previewTextMarksMissingDurationAsInvalid() {
        String text = "课程 A | | Java后端开发 | 其他 | https://example.com/a |";

        CourseImportPreview preview = service.previewText(text);

        assertThat(preview.validCount()).isZero();
        assertThat(preview.invalidCount()).isEqualTo(1);
        assertThat(preview.token()).isNull();
        assertThat(preview.rows().get(0).status()).isEqualTo("invalid");
    }

    private Subject subject(long id, String name) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName(name);
        return subject;
    }
}
