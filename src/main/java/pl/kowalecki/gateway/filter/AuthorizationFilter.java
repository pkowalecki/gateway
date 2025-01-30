package pl.kowalecki.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import pl.kowalecki.gateway.sercurity.JwtUtils;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> {

    private final RouteValidator routeValidator;
    private final JwtUtils jwtUtils;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public AuthorizationFilter(RouteValidator routeValidator, JwtUtils jwtUtils, WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.routeValidator = routeValidator;
        this.jwtUtils = jwtUtils;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (!routeValidator.isSecured.test(exchange.getRequest())) {
                log.info("Open route accessed: {}", exchange.getRequest().getURI());
                return chain.filter(exchange);
            }
            log.info("Robie requa do: {}", exchange.getRequest().getURI());
            log.info("headers: {}", exchange.getRequest().getHeaders().toString());
            //todo access token bearer gdzieś się tworzy i przekazuje z danymi refreshtokenu
            String accessToken = extractJwt(exchange.getRequest());
            String refreshToken = extractRefreshToken(exchange.getRequest());

            try {
                if (accessToken == null) {
                    log.info("Access token is null");
                    if (refreshToken != null && validateRefreshToken(refreshToken)) {
                        log.info("Refresh token is valid so i do requset");
                        return refreshAccessToken(exchange, refreshToken, chain);
                    }
                    return unauthorizedResponse(exchange, "Authorization header is missing or invalid");
                }

                if (jwtUtils.validateJwtToken(accessToken)) {
                    //todo missing user id? Wywalamy
                    String userId = jwtUtils.extractUserId(accessToken);
                    String email = jwtUtils.extractEmail(accessToken);
                    log.info("Adding headers: X-User-Id={}, X-User-Email={}", userId, email);

                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Email", email)
                            .build();

                    return chain.filter(exchange.mutate().request(request).build());
                }
                if (refreshToken != null && validateRefreshToken(refreshToken)) {
                    log.info("Refresh token is valid wiec odswiezam accesstoken");
                    return refreshAccessToken(exchange, refreshToken, chain);
                }

                return unauthorizedResponse(exchange, "Authorization header is missing or invalid");
            } catch (Exception e) {
                log.error("Authorization failed: {}", e.getMessage());
                return unauthorizedResponse(exchange, "Authorization failed");
            }
        });
    }

    private String extractJwt(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
    }

    private String extractRefreshToken(ServerHttpRequest request) {
        String refreshTokenHeader = request.getHeaders().getFirst("X-Refresh-Token");
        return refreshTokenHeader != null ? refreshTokenHeader : null;
    }

    private boolean validateRefreshToken(String refreshToken) {
        try {
            jwtUtils.validateJwtToken(refreshToken);
            return true;
        } catch (Exception e) {
            log.error("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    private Mono<Void> refreshAccessToken(ServerWebExchange exchange, String refreshToken, GatewayFilterChain chain) {
        WebClient webClient = webClientBuilder.baseUrl("http://authorization-server").build();
        log.info("Wołam o odświeżenie ciasteczka!");
        return webClient.post()
                .uri("/api/v1/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(tokens -> {
                    String newAccessToken = (String) tokens.get("accessToken");
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken)
                            .build();
                    log.info("Successfully refreshed access token: {}", newAccessToken);
                    return chain.filter(exchange.mutate().request(request).build());
                })
                .onErrorResume(e -> unauthorizedResponse(exchange, "Failed to refresh token"));
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(("{\"error\":\"" + message + "\"}").getBytes());
        log.info("Unauthorized response: {}", buffer);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Config {

    }
}
