package pl.kowalecki.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(0)
public class LoggingGlobalFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("====================================================================================");
        log.info("Request URI: {}", exchange.getRequest().getURI());
        log.info("HTTP Method: {}", exchange.getRequest().getMethod());
        logHeaders("Request Headers", exchange.getRequest().getHeaders());
        return chain.filter(exchange)
                .doOnSuccess(aVoid -> {
                    log.info("Response Status: {}", exchange.getResponse().getStatusCode());
                    logHeaders("Response Headers", exchange.getResponse().getHeaders());
                    log.info("====================================================================================");
                })
                .doOnError(errResume -> {
                    log.info("Error Status: {}", exchange.getResponse().getStatusCode());
                    logHeaders("Error Response Headers", exchange.getResponse().getHeaders());
                })
                .then();
    }

    private void logHeaders(String headerType, HttpHeaders headers) {
        log.info("{}:", headerType);
        headers.forEach((key, value) -> log.info("{}: {}", key, value));
    }
}
