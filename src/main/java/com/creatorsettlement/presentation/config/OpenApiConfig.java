package com.creatorsettlement.presentation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI creatorSettlementOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Creator Settlement API")
                .version("v0")
                .description("크리에이터 월별 정산용 매출/취소 API"));
    }
}
