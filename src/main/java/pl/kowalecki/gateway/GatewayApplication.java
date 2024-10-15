package pl.kowalecki.gateway;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "API Gateway", version = "1.0", description = "Documentation API Gateway v1.0"))
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("authorization-server", r->
                        r.path("/**")
                                .uri("lb://authorization-server"))
                .route("eureka-server", r ->
                        r.path("/eureka/main")
                                .filters(f -> f.setPath("/"))
                                .uri("http://eureka-server:8761"))
                .route("eureka-server-static", r ->
                        r.path("/eureka/**")
                                .uri("http://eureka-server:8761"))
                .route("diet-planner-web", r ->
                        r.path("/app/**")
                                .uri("http://diet-planner-web:8081"))
                .route("diet-planner-web-static", r ->
                        r.path("/static/**")
                                .uri("http://diet-planner-web:8081"))
                .route("diet-planner-api", r ->
                        r.path("/api/v1/dpa/**")
                                .filters(f -> f.stripPrefix(2))
                                .uri("lb://diet-planner-api"))
                .build();

    }


}
