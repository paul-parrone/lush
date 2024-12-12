package com.px3j.service.show;

import com.px3j.lush.core.model.AnyModel;
import reactor.core.publisher.Mono;

public interface RemoteService {
    Mono<AnyModel> ping(String ticket);
}
