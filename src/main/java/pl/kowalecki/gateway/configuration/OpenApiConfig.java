package pl.kowalecki.gateway.configuration;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    GroupedOpenApi dietPlannerApi() {
        return GroupedOpenApi.builder()
                .group("diet-planner-api")
                .pathsToMatch("/api/v1/dpa/**")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("Diet Planner API")))
                .build();
    }
}
