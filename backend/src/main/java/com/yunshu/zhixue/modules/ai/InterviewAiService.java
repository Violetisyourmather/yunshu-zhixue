package com.yunshu.zhixue.modules.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.modules.ai.dto.InterviewAnswerGrade;
import com.yunshu.zhixue.modules.ai.dto.InterviewImproveCard;
import com.yunshu.zhixue.modules.ai.dto.InterviewQaSnapshot;
import com.yunshu.zhixue.modules.ai.dto.InterviewReportAi;
import com.yunshu.zhixue.modules.ai.dto.ShortAnswerGrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 面试评分与报告生成。
 *
 * <p>逐题评分复用简答题批改能力；整场报告一次调用模型汇总六维度、优缺点、建议与提升卡。
 * 模型不可用时使用基于逐题得分与参考答案的本地降级报告。</p>
 */
@Service
public class InterviewAiService {

    private static final Logger log = LoggerFactory.getLogger(InterviewAiService.class);

    private static final String DEFAULT_BASE_URL = "https://api.xiaomimimo.com/v1";
    private static final String[] DIMENSIONS = {
            "技术准确性", "知识广度", "原理理解", "工程实践", "逻辑结构", "表达清晰"
    };
    private static final double[] WEIGHTS = {0.30, 0.10, 0.20, 0.20, 0.10, 0.10};

    private static final String REPORT_SYSTEM_PROMPT = """
            你是一名资深技术面试官与职业教练。请根据面试问答记录生成一份复盘报告。
            只输出 JSON，不要 Markdown 代码块，不要额外解释。JSON 结构：
            {
              "dimensions": {
                "技术准确性": 0-10的数字,
                "知识广度": 0-10的数字,
                "原理理解": 0-10的数字,
                "工程实践": 0-10的数字,
                "逻辑结构": 0-10的数字,
                "表达清晰": 0-10的数字
              },
              "strengths": ["优点1", "优点2"],
              "weaknesses": ["问题描述 + 一句指导性建议", "问题描述 + 一句指导性建议"],
              "suggestions": ["建议1", "建议2", "建议3"],
              "improvementCards": [
                {"title": "推荐题目", "content": "具体指导", "type": "bank"},
                {"title": "推荐课程", "content": "具体指导", "type": "route"},
                {"title": "推荐再次面试", "content": "具体指导", "type": "interview"}
              ]
            }
            维度权重：技术准确性30%、知识广度10%、原理理解20%、工程实践20%、逻辑结构10%、表达清晰10%。
            每条 weaknesses 必须写成「问题描述 + 一句指导性建议」两段，先指出具体问题，再用一句话说明怎么改。
            评价要具体、可执行，避免空泛鼓励。
            """;

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final ShortAnswerGrader shortAnswerGrader;
    private final RestClient restClient;

    public InterviewAiService(AiProperties properties,
                              ObjectMapper objectMapper,
                              ShortAnswerGrader shortAnswerGrader) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.shortAnswerGrader = shortAnswerGrader;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(Math.max(properties.getTimeoutSeconds(), 10)));
        this.restClient = RestClient.builder()
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .requestFactory(factory)
                .build();
    }

    /** 逐题评分复用简答题 AI 批改。 */
    public InterviewAnswerGrade gradeAnswer(String question, String referenceAnswer, String studentAnswer) {
        ShortAnswerGrade grade = shortAnswerGrader.grade(question, referenceAnswer, studentAnswer);
        return new InterviewAnswerGrade(
                grade.score(), grade.feedback(), grade.points(), grade.degraded());
    }

    /** 整场面试结束后生成六维报告。 */
    public InterviewReportAi generateReport(String subjectName, int difficulty,
                                            List<InterviewQaSnapshot> snapshots) {
        double average = snapshots.stream().mapToDouble(InterviewQaSnapshot::score).average().orElse(0);
        if (!StringUtils.hasText(properties.getApiKey())) {
            return fallback(subjectName, average);
        }
        try {
            String userPrompt = buildReportPrompt(subjectName, difficulty, snapshots);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.getModel());
            body.put("messages", List.of(
                    Map.of("role", "system", "content", REPORT_SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userPrompt)));
            body.put("temperature", 0.3);
            body.put("max_tokens", 1600);
            body.put("thinking", Map.of("type", "disabled"));

            String raw = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(raw)) {
                return fallback(subjectName, average);
            }
            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            String json = extractJson(content);
            if (!StringUtils.hasText(json)) {
                return fallback(subjectName, average);
            }
            JsonNode payload = objectMapper.readTree(json);
            Map<String, Double> dimensions = parseDimensions(payload.path("dimensions"), average);
            List<String> strengths = parseStringList(payload.path("strengths"));
            List<String> weaknesses = parseStringList(payload.path("weaknesses"));
            List<String> suggestions = parseStringList(payload.path("suggestions"));
            List<InterviewImproveCard> cards = parseCards(payload.path("improvementCards"), subjectName);
            if (strengths.isEmpty()) {
                strengths = fallbackStrengths(subjectName, average);
            }
            if (weaknesses.isEmpty()) {
                weaknesses = fallbackWeaknesses(average);
            }
            if (suggestions.isEmpty()) {
                suggestions = fallbackSuggestions();
            }
            return new InterviewReportAi(dimensions, strengths, weaknesses, suggestions,
                    cards, overall(dimensions), false);
        } catch (Exception ex) {
            log.warn("AI 面试报告生成失败，降级为本地报告：{}", ex.getMessage());
            return fallback(subjectName, average);
        }
    }

    private String buildReportPrompt(String subjectName, int difficulty,
                                     List<InterviewQaSnapshot> snapshots) {
        StringBuilder builder = new StringBuilder();
        builder.append("方向：").append(subjectName).append('\n');
        builder.append("难度：").append(difficultyLabel(difficulty)).append('\n');
        builder.append("问答记录：\n");
        for (InterviewQaSnapshot snapshot : snapshots) {
            builder.append(snapshot.order()).append(". 题目：").append(limit(snapshot.question(), 800)).append('\n');
            builder.append("参考答案：").append(limit(snapshot.referenceAnswer(), 600)).append('\n');
            builder.append("学员回答：").append(limit(snapshot.studentAnswer(), 1500)).append('\n');
            builder.append("本题得分：").append(snapshot.score()).append("/10；评语：")
                    .append(limit(snapshot.feedback(), 500)).append('\n');
        }
        builder.append("请生成复盘报告。");
        return builder.toString();
    }

    private Map<String, Double> parseDimensions(JsonNode node, double average) {
        Map<String, Double> fallback = fallbackDimensions(average);
        Map<String, Double> result = new LinkedHashMap<>();
        for (String name : DIMENSIONS) {
            JsonNode value = node.path(name);
            double score = value.isNumber() ? value.asDouble() : fallback.getOrDefault(name, 0.0);
            result.put(name, round(Math.max(0, Math.min(10, score))));
        }
        return result;
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String text = item.asText("").trim();
                if (!text.isEmpty()) {
                    result.add(limit(text, 300));
                }
                if (result.size() >= 5) {
                    break;
                }
            }
        }
        return result;
    }

    private List<InterviewImproveCard> parseCards(JsonNode node, String subjectName) {
        List<InterviewImproveCard> fallback = fallbackCards(subjectName);
        List<InterviewImproveCard> result = new ArrayList<>();
        String[] types = {"bank", "route", "interview"};
        if (node.isArray()) {
            int index = 0;
            for (JsonNode item : node) {
                String title = item.path("title").asText("").trim();
                String content = item.path("content").asText("").trim();
                String type = item.path("type").asText("").trim();
                if (title.isEmpty()) {
                    title = fallback.get(Math.min(index, fallback.size() - 1)).title();
                }
                if (content.isEmpty()) {
                    content = fallback.get(Math.min(index, fallback.size() - 1)).content();
                }
                if (type.isEmpty()) {
                    type = types[Math.min(index, types.length - 1)];
                }
                result.add(new InterviewImproveCard(limit(title, 50), limit(content, 500), type));
                index++;
                if (result.size() >= 3) {
                    break;
                }
            }
        }
        while (result.size() < 3) {
            result.add(fallback.get(result.size()));
        }
        return result;
    }

    private InterviewReportAi fallback(String subjectName, double average) {
        Map<String, Double> dimensions = fallbackDimensions(average);
        return new InterviewReportAi(
                dimensions,
                fallbackStrengths(subjectName, average),
                fallbackWeaknesses(average),
                fallbackSuggestions(),
                fallbackCards(subjectName),
                overall(dimensions),
                true);
    }

    private Map<String, Double> fallbackDimensions(double average) {
        Map<String, Double> dimensions = new LinkedHashMap<>();
        dimensions.put("技术准确性", clamp(average + 0.4));
        dimensions.put("知识广度", clamp(average - 0.3));
        dimensions.put("原理理解", clamp(average + 0.1));
        dimensions.put("工程实践", clamp(average - 0.2));
        dimensions.put("逻辑结构", clamp(average + 0.2));
        dimensions.put("表达清晰", clamp(average - 0.5));
        return dimensions;
    }

    private List<String> fallbackStrengths(String subjectName, double average) {
        if (average >= 7.5) {
            return List.of("对「" + subjectName + "」方向的核心概念掌握较好",
                    "能围绕问题给出较完整的实现思路", "回答有条理，结论相对清晰");
        }
        if (average >= 6) {
            return List.of("具备基本的技术理解", "能够完成多数问题的作答", "回答方向大体正确");
        }
        return List.of("已经完成本次面试，具备继续练习的基础", "能识别部分关键概念");
    }

    private List<String> fallbackWeaknesses(double average) {
        if (average < 6) {
            return List.of("基础概念和关键术语还不够准确", "回答缺少实现细节与边界处理", "部分题目没有形成完整结论");
        }
        return List.of("边界与失败场景考虑不足", "原理层面的取舍讲得不够", "部分回答可以更简洁、结论更前置");
    }

    private List<String> fallbackSuggestions() {
        return List.of("回答先给结论，再补两个支撑点，控制在 90 秒内",
                "每道题补一句失败场景与降级方案，例如超时、参数校验失败、检索为空",
                "把「怎么用」升级成「为什么这样设计」，至少讲清一个取舍");
    }

    private List<InterviewImproveCard> fallbackCards(String subjectName) {
        return List.of(
                new InterviewImproveCard("推荐题目",
                        "回到「" + subjectName + "」题库，重点练习本次低分题目对应阶段的内容，先补概念再练表达。",
                        "bank"),
                new InterviewImproveCard("推荐课程",
                        "在学习路线中复习「" + subjectName + "」方向的核心课程，重点关注工程实践与边界处理。",
                        "route"),
                new InterviewImproveCard("推荐再次面试",
                        "建议 3 天后再次进行同方向、同难度的面试，重点观察原理理解与工程实践维度。",
                        "interview"));
    }

    private static double overall(Map<String, Double> dimensions) {
        double sum = 0;
        for (int index = 0; index < DIMENSIONS.length; index++) {
            sum += dimensions.getOrDefault(DIMENSIONS[index], 0.0) * WEIGHTS[index];
        }
        return round(Math.max(0, Math.min(10, sum)));
    }

    private static String difficultyLabel(int difficulty) {
        return switch (difficulty) {
            case 1 -> "简单";
            case 3 -> "困难";
            default -> "中等";
        };
    }

    private static double clamp(double value) {
        return round(Math.max(0, Math.min(10, value)));
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String extractJson(String content) {
        if (content == null) {
            return "";
        }
        String value = content.trim();
        if (value.startsWith("```")) {
            int firstNewline = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                value = value.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
        }
        return value;
    }

    private static String normalizeBaseUrl(String value) {
        String base = StringUtils.hasText(value) ? value.trim() : DEFAULT_BASE_URL;
        return base.replaceAll("/+$", "");
    }
}
