package com.px3j.service.show.impl;

import com.px3j.lush.core.model.AnyModel;
import com.px3j.lush.web.common.Constants;
import com.px3j.service.show.RemoteService;
import feign.Headers;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;

@ReactiveFeignClient(name = "exampleService", url = "${lush.test.remote-ping-url}")
//@FeignClient(name = "exampleService", url = "${CHANGE-TO-REMOTE-SERVICE-NAME}")
@Headers({ "Accept: application/json" })
public interface FeignRemoteServiceImpl extends RemoteService {
    @RequestMapping(method = RequestMethod.GET, value = "/lush/example/ping")
    Mono<AnyModel> ping(@RequestHeader(Constants.TICKET_HEADER_NAME) String ticket);
}
