/**
 * AI 能力层（S3）：简答批改、面试逐题评分、报告汇总。
 *
 * <p>无状态、不依赖任何业务表、不落库：只接收入参并返回结构化结果，
 * 由调用方（{@code study} / {@code interview}）负责持久化。</p>
 *
 * <p>模型接入遵循 OpenAI 兼容协议（D15），provider 仅境内调用；
 * base-url / model / api-key 走配置与环境变量，不落数据库。</p>
 *
 * <p>约定：统一超时、重试与降级（R1）；要求结构化 JSON 输出并做容错解析；
 * 提示词模板位于 {@code resources/prompts}。</p>
 */
package com.yunshu.zhixue.modules.ai;
