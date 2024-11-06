package pl.kowalecki.gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import pl.kowalecki.gateway.sercurity.JwtUtil;

import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

@Component
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public AuthorizationFilter(RouteValidator validator, JwtUtil jwtUtil, WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (validator.isSecured.test(exchange.getRequest())) {
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return unauthorizedResponse(exchange, "Authorization header is missing");
                }

                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                } else {
                    return unauthorizedResponse(exchange, "Invalid Authorization header format");
                }

                try {
                    jwtUtil.validateToken(authHeader);
                } catch (Exception e) {
                    String refreshToken = exchange.getRequest().getCookies().getFirst("refreshToken") != null
                            ? exchange.getRequest().getCookies().getFirst("refreshToken").getValue()
                            : null;
                    if (refreshToken == null) {
                        return unauthorizedResponse(exchange, "Refresh token is missing");
                    }

                    return getNewTokenFromAuthService(refreshToken)
                            .flatMap(newAuthToken -> {
                                exchange.getRequest().mutate().header(HttpHeaders.AUTHORIZATION, "Bearer " + newAuthToken);
                                return chain.filter(exchange);
                            })
                            .onErrorResume(error -> unauthorizedResponse(exchange, "Unauthorized access - token refresh failed!"));
                }
            }
            return chain.filter(exchange);
        });
    }

    private Mono<String> getNewTokenFromAuthService(String refreshToken) {
        return webClientBuilder.build()
                .post()
                .uri("lb://authorization-server/api/v1/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("Failed to refresh token")))
                .bodyToMono(String.class);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(("{\"error\":\"" + message + "\"}").getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Config {

    }
}
