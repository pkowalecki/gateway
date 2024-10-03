package pl.kowalecki.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("public", r -> r.path("/api/v1/dietplanner/login", "/api/v1/dietplanner/register")
                        .uri("lb://diet-planner-api"))
                .route("diet-planner-api", r -> r.path("/api/v1/dietplanner/**")
//                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://diet-planner-api"))
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
