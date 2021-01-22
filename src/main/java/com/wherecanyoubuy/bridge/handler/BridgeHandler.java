package com.wherecanyoubuy.bridge.handler;

import com.wherecanyoubuy.bridge.entity.BridgeRequestEntity;
import com.wherecanyoubuy.bridge.service.ScraperService;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

@Builder
public class BridgeHandler {
    @Autowired
    private ScraperService scraperService;

    public Mono<ServerResponse> search(ServerRequest serverRequest) {
        return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(serverRequest
                        .bodyToFlux(BridgeRequestEntity.class)
                        .flatMap(bridgeRequestEntity ->
                                scraperService.search(bridgeRequestEntity)), List.class);
    }
}
