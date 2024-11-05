package pl.kowalecki.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(0)
public class LoggingGlobalFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("---------------");
        log.info("Wykonywany request do: {}", exchange.getRequest().getURI());
        log.info("---------------");

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    log.info("Response status: {}", exchange.getResponse().getStatusCode());
                }));
    }

}
