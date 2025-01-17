package com.px3j.service.show.impl;

import com.px3j.lush.core.model.AnyModel;
import com.px3j.lush.web.common.Constants;
import com.px3j.service.show.RemoteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RestRemoteServiceImpl implements RemoteService {
    private final String baseUrl;
    private final WebClient webClient;

    public RestRemoteServiceImpl(@Value("${lush.test.remote-ping-url}") String baseUrl, WebClient webClient) {
        this.baseUrl = baseUrl;
        this.webClient = webClient;
    }

    public Mono<AnyModel> ping(String ticket) {
        return webClient.get()
                .uri(baseUrl + "/lush/example/ping")
                .header("Accept", "application/json")
                .header(Constants.TICKET_HEADER_NAME, ticket)
                .retrieve()
                .bodyToMono(AnyModel.class);
    }
}
