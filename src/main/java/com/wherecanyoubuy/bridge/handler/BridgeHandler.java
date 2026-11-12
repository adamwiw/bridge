package com.wherecanyoubuy.bridge.handler;

import com.wherecanyoubuy.bridge.entity.ElementQueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.ExtractRequest;
import com.wherecanyoubuy.bridge.entity.FetchRequest;
import com.wherecanyoubuy.bridge.entity.RegexQueryRequestEntity;
import com.wherecanyoubuy.bridge.service.ScraperService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@AllArgsConstructor
@Component
public class BridgeHandler {
    private final ScraperService scraperService;

    public Mono<ServerResponse> search(ServerRequest serverRequest) {
        return serverRequest
                .bodyToFlux(ElementQueryRequestEntity.class)
                .parallel()
                .runOn(Schedulers.newBoundedElastic(20, 10, "ScraperThreadGroup"))
                .flatMap(scraperService::search)
                .sequential()
                .collectList()
                .flatMap(maps -> ServerResponse.ok().bodyValue(maps))
                .onErrorResume(this::errorResponse);
    }

    public Mono<ServerResponse> searchRegex(ServerRequest serverRequest) {
        return serverRequest
                .bodyToFlux(RegexQueryRequestEntity.class)
                .concatMap(scraperService::search)
                .collectList()
                .flatMap(maps -> ServerResponse.ok().bodyValue(maps))
                .onErrorResume(this::errorResponse);
    }

    public Mono<ServerResponse> sessionOpen(ServerRequest serverRequest) {
        return serverRequest
                .bodyToMono(FetchRequest.class)
                .flatMap(scraperService::openSession)
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(this::errorResponse);
    }

    public Mono<ServerResponse> sessionExtract(ServerRequest serverRequest) {
        return serverRequest
                .bodyToMono(ExtractRequest.class)
                .flatMap(scraperService::extractSession)
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(this::errorResponse);
    }

    public Mono<ServerResponse> sessionClose(ServerRequest serverRequest) {
        return serverRequest
                .bodyToMono(ExtractRequest.class)
                .flatMap(request -> scraperService.closeSession(request.getSessionId()))
                .flatMap(closed -> ServerResponse.ok().bodyValue(java.util.Map.of("closed", closed)))
                .onErrorResume(this::errorResponse);
    }

    public Mono<ServerResponse> sessions(ServerRequest serverRequest) {
        return scraperService.sessionCount()
                .flatMap(open -> ServerResponse.ok().bodyValue(java.util.Map.of("open", open)));
    }

    private Mono<ServerResponse> errorResponse(Throwable throwable) {
        log.warn("bridge error", throwable);
        String msg = throwable.getMessage() == null ? "" : throwable.getMessage().toLowerCase();

        boolean isProxyFailure = msg.contains("err_tunnel_connection_failed")
                || msg.contains("err_proxy_connection_failed")
                || msg.contains("proxy failed")
                || msg.contains("proxy authentication")
                || msg.contains("authentication");

        boolean isUnreachableSite = msg.contains("err_connection_reset")
                || msg.contains("err_name_not_resolved")
                || msg.contains("err_connection_timed_out")
                || msg.contains("err_empty_response")
                || msg.contains("err_aborted")
                || msg.contains("err_ssl_protocol_error")
                || msg.contains("err_address_unreachable")
                || msg.contains("timeout")
                || msg.contains("refused")
                || msg.contains("unreachable")
                || msg.contains("page content too short")
                || msg.contains("could not start a new session")
                || msg.contains("invalid address of the remote server")
                || msg.contains("browser start-up failure")
                || msg.contains("error communicating with the remote browser")
                || msg.contains("may have died");

        if (isProxyFailure) {
            return ServerResponse.status(HttpStatus.GATEWAY_TIMEOUT).bodyValue("proxy error " + msg);
        }
        if (isUnreachableSite) {
            return ServerResponse.status(HttpStatus.GATEWAY_TIMEOUT).bodyValue("unreachable error " + msg);
        }
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue("unknown");
    }
}
