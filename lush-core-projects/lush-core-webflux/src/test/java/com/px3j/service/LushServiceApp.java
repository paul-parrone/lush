package com.px3j.service;

import com.px3j.lush.webflux.EnableLushWebflux;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.spring.config.EnableReactiveFeignClients;
import reactor.core.publisher.Mono;

/**
 * This is a simple Spring Boot application showing how to use Lush in your application.
 *
 * @author Paul Parrone
 */
@Slf4j
@SpringBootApplication
@EnableReactiveFeignClients
@EnableLushWebflux
public class LushServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(LushServiceApp.class, args);
    }

    @Bean
    WebClient webClient(WebClient.Builder builder) {
        return builder
                .filter(this::logRequestAndResponse) // Apply the custom logging filter
                .build();
    }


    // Filter to log request and response
    private Mono<ClientResponse> logRequestAndResponse(ClientRequest request, ExchangeFunction next) {
        // Log request details
        log.info("Request: {} {}", request.method(), request.url());
        request.headers().forEach((name, values) -> values.forEach(value -> log.info("Header: {}={}", name, value)));

        // Proceed with the next filter and log response details
        return next.exchange(request)
                .doOnNext(response -> {
                    log.info("Response Status: {}", response.statusCode());
                    response.headers().asHttpHeaders().forEach((name, values) ->
                            values.forEach(value -> log.info("Response Header: {}={}", name, value))
                    );
                });
    }
}
