package pl.kowalecki.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        Logger logger = LoggerFactory.getLogger(GatewayApplication.class);

        return builder.routes()
                .route("diet-planner-api", r -> r
                        .path("/api/v1/**")
                        .filters(f -> {
                            logger.info("Request passing through Gateway ");
                            return f;
                        })
                        .uri("lb://diet-planner-api/"))
                .build();

    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
