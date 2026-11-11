package com.yunshu.zhixue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 契约（D21）：springdoc 导出的 openapi.json 是两端类型生成的唯一来源。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI yunshuZhixueOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("云枢智学 API")
                        .version("v1")
                        .description("学员端 /api/portal/v1/**；管理端 /api/admin/v1/**；两端 JWT 互不通用"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
