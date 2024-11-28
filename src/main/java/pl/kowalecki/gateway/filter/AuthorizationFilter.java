package pl.kowalecki.gateway.filter;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import pl.kowalecki.gateway.sercurity.JwtUtil;

import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthorizationFilter(RouteValidator validator, JwtUtil jwtUtil) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (!validator.isSecured.test(exchange.getRequest())) {
                log.info("---------------");
                log.info("OPEN ROUTE: {}",  exchange.getRequest().getURI());
                log.info("---------------");
                return chain.filter(exchange);
            }
            String authHeader = extractAuthHeader(exchange);
            if (authHeader == null) {
                return unauthorizedResponse(exchange, "Authorization header is missing or invalid");
            }
            try {
                jwtUtil.validateToken(authHeader);
                String userId = jwtUtil.extractUserId(authHeader);
                String email = jwtUtil.extractEmail(authHeader);
                ServerHttpRequest request = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Email", email)
                        .build();
                log.info("REQUEST {}",  request);
                return chain.filter(exchange.mutate().request(request).build());
            } catch (Exception e) {
                return unauthorizedResponse(exchange, "Invalid or expired token");
            }
        });
    }

    private String extractAuthHeader(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
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
