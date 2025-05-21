package com.wherecanyoubuy.bridge.handler;

import com.wherecanyoubuy.bridge.entity.ElementQueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.RegexQueryRequestEntity;
import com.wherecanyoubuy.bridge.service.ScraperService;
import lombok.AllArgsConstructor;
import org.openqa.selenium.WebDriverException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.function.Predicate.isEqual;

@AllArgsConstructor
@Component
public class BridgeHandler {
    private ScraperService scraperService;

    public Mono<ServerResponse> search(ServerRequest serverRequest) {
        return serverRequest
                .bodyToFlux(ElementQueryRequestEntity.class)
                .parallel()
                .runOn(Schedulers.newBoundedElastic(20, 10, "ScraperThreadGroup"))
                .flatMap(elementQueryRequestEntity -> scraperService.search(elementQueryRequestEntity))
                .sequential()
                .collectList()
                .flatMap(maps -> ServerResponse
                        .ok()
                        .bodyValue(maps))
                .onErrorResume(throwable -> {
                    String msg = throwable.getMessage().toLowerCase();

                    boolean isProxyFailure =
                            throwable instanceof WebDriverException && msg.contains("err_tunnel_connection_failed") ||
                                    throwable instanceof WebDriverException && msg.contains("err_proxy_connection_failed") ||
                                    msg.contains("proxy failed") ||
                                    msg.contains("proxy authentication") ||
                                    msg.contains("authentication");

                    boolean isUnreachableSite =
                            msg.contains("err_connection_reset") ||
                                    msg.contains("err_name_not_resolved") ||
                                    msg.contains("err_connection_timed_out") ||
                                    msg.contains("err_empty_response") ||
                                    msg.contains("err_aborted") ||
                                    msg.contains("err_ssl_protocol_error") ||
                                    msg.contains("err_address_unreachable") ||
                                    msg.contains("timeout") ||
                                    msg.contains("refused") ||
                                    msg.contains("unreachable") ||
                                    msg.contains("page content too short") ||
                                    msg.contains("could not start a new session") ||
                                    msg.contains("invalid address of the remote server") ||
                                    msg.contains("browser start-up failure") ||
                                    msg.contains("error communicating with the remote browser") ||  // ← NEW
                                    msg.contains("may have died");

                    if (isProxyFailure) {
                        System.out.println("⚠️ Proxy-related failure: " + msg);
                        return ServerResponse.status(HttpStatus.GATEWAY_TIMEOUT).bodyValue(String.format("proxy error %s", msg));
                    } else if (isUnreachableSite) {
                        System.out.println("💤 Site unreachable: " + msg);
                        return ServerResponse.status(HttpStatus.GATEWAY_TIMEOUT).bodyValue(String.format("unreachable error %s", msg));
                    } else {
                        System.out.println("❌ Unknown error: " + msg);
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue("unknown");
                    }
                });
    }

    public Mono<ServerResponse> searchRegex(ServerRequest serverRequest) {
        return ServerResponse
                .ok()
                .body(serverRequest
                                .bodyToFlux(RegexQueryRequestEntity.class)
                                .concatMap(regexQueryRequestEntity -> scraperService.search(regexQueryRequestEntity)),
                        List.class);
    }
}
