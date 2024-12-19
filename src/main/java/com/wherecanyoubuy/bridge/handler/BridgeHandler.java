package com.wherecanyoubuy.bridge.handler;

import com.wherecanyoubuy.bridge.entity.ElementQueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.RegexQueryRequestEntity;
import com.wherecanyoubuy.bridge.service.ScraperService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@AllArgsConstructor
@Component
public class BridgeHandler {
    private ScraperService scraperService;

    public Mono<ServerResponse> search(ServerRequest serverRequest) {
        return ServerResponse
                .ok()
                .body(serverRequest
                        .bodyToFlux(ElementQueryRequestEntity.class)
                        .parallel()
                        .runOn(Schedulers.newBoundedElastic(20, 10, "ScraperThreadGroup"))
                        .flatMap(elementQueryRequestEntity ->
                                scraperService.search(elementQueryRequestEntity)), List.class);
    }
    public Mono<ServerResponse> searchRegex(ServerRequest serverRequest) {
        return ServerResponse
                .ok()
                .body(serverRequest
                        .bodyToFlux(RegexQueryRequestEntity.class)
                        .concatMap(regexQueryRequestEntity ->
                                scraperService.search(regexQueryRequestEntity)), List.class);
    }
}
