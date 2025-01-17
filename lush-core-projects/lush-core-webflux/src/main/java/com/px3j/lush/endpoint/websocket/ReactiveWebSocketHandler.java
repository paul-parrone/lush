package com.px3j.lush.endpoint.websocket;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j( topic = "lush.core.debug" )
public class ReactiveWebSocketHandler implements WebSocketHandler {

    private Flux<String> intervalFlux;

    @PostConstruct
    private void setup() {
        intervalFlux = Flux.interval(Duration.ofSeconds(2)).map(it -> getEvent());
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.send(
                session
                        .receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .log()
                        .map(session::textMessage));
    }

    private String getEvent() {
//        JsonNode node = mapper.valueToTree(new Event(messageGenerator.generate(), Instant.now()));
//        return node.toString();
        log.debug( "Instant: {}", Instant.now().toString() );

        return "{\"message\":\"hello\"}";
    }
}