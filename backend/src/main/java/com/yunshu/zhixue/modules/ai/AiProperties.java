package com.yunshu.zhixue.modules.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模型配置（D15，OpenAI 兼容协议）。
 *
 * <p>base-url / model / api-key 全部走配置与环境变量，不落数据库、不写入前端。</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String baseUrl = "https://api.xiaomimimo.com/v1";

    private String apiKey = "";

    private String model = "mimo-v2.6-pro";

    /** 单次请求读超时（秒）。 */
    private int timeoutSeconds = 60;
}
