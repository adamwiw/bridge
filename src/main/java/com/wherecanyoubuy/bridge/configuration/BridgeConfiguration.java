package com.wherecanyoubuy.bridge.configuration;

import com.wherecanyoubuy.bridge.handler.BridgeHandler;
import com.wherecanyoubuy.bridge.service.ScraperService;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
public class BridgeConfiguration {
    @Bean
    public RouterFunction<ServerResponse> route() {
        BridgeHandler bridgeHandler = BridgeHandler
                .builder()
                .scraperService(ScraperService
                        .builder()
                        .applicationContext(new DefaultListableBeanFactory())
                        .build())
                .build();

        return RouterFunctions
                .route(POST("/bridge/search").and(accept(APPLICATION_JSON)), bridgeHandler::search);
    }
}
