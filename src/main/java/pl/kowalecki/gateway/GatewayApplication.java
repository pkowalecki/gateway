package pl.kowalecki.gateway;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import pl.kowalecki.gateway.filter.AuthorizationFilter;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "API Gateway", version = "1.0", description = "Documentation API Gateway v1.0"))
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, AuthorizationFilter authorizationFilter) {
        return builder.routes()
                .route("eureka-server", r -> r.path("/eureka/main")
                                .filters(f -> f.setPath("/"))
                                .uri("http://eureka-server:8761"))
                .route("eureka-server-static", r ->
                        r.path("/eureka/**")
                                .uri("http://eureka-server:8761"))
                .route("auth-service", r ->
                        r.path("/api/v1/auth/**").filters(f -> f.filter(authorizationFilter.apply(new AuthorizationFilter.Config())))
                                .uri("lb://authorization-server"))
                .route("diet-planner-api", r ->
                        r.path("/api/v1/dpa/**").filters(f -> f.stripPrefix(3).filter(authorizationFilter.apply(new AuthorizationFilter.Config())))
                                .uri("lb://diet-planner-api"))
                .build();

    }
}
