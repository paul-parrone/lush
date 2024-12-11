package com.px3j.service.show.impl;

import com.px3j.service.show.RemoteService;
import com.px3j.lush.core.model.AnyModel;
import com.px3j.lush.web.common.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RestRemoteServiceImpl implements RemoteService {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RestRemoteServiceImpl(RestTemplate restTemplate,
                                 @Value("${lush.test.remote-ping-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public AnyModel ping(String ticket) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set(Constants.TICKET_HEADER_NAME, ticket);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = baseUrl + "/lush/show/ping";

        ResponseEntity<AnyModel> response = restTemplate.exchange(url, HttpMethod.GET, entity, AnyModel.class);
        return response.getBody();
    }
}
